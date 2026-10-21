package pl.darkhud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HudRenderer {
	public static final int ACCENT = 0xFF8B7CFF;
	public static final int LABEL = 0xFF9A9AB0;
	public static final int VALUE = 0xFFF2F2F7;
	public static final int PANEL_BG = 0xC0101015;

	private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();
	private static boolean leftWasDown = false;
	private static boolean rightWasDown = false;

	private record Line(String label, String value) {}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}

		updateCps(mc);

		if (mc.options.hideGui) {
			return;
		}

		HudConfig cfg = HudConfig.INSTANCE;
		List<Line> lines = buildLines(mc, cfg);
		if (lines.isEmpty()) {
			return;
		}

		drawPanel(g, mc, lines, cfg);
	}

	private static List<Line> buildLines(Minecraft mc, HudConfig cfg) {
		List<Line> lines = new ArrayList<>();

		if (cfg.showNick) {
			lines.add(new Line("Nick", mc.getUser().getName()));
		}
		if (cfg.showServer) {
			lines.add(new Line("Serwer", serverName(mc)));
		}
		if (cfg.showFps) {
			lines.add(new Line("FPS", String.valueOf(mc.getFps())));
		}
		if (cfg.showPing) {
			lines.add(new Line("Ping", pingText(mc)));
		}
		if (cfg.showCps) {
			lines.add(new Line("CPS", cps(LEFT_CLICKS) + " | " + cps(RIGHT_CLICKS)));
		}
		if (cfg.showCoords && !cfg.coordsHidden) {
			lines.add(new Line("XYZ", String.format(Locale.ROOT, "%.1f / %.1f / %.1f",
					mc.player.getX(), mc.player.getY(), mc.player.getZ())));
		}
		if (cfg.showNether && !cfg.coordsHidden) {
			var dim = mc.level.dimension();
			if (dim == Level.OVERWORLD) {
				lines.add(new Line("Nether", String.format(Locale.ROOT, "%.1f / %.1f",
						mc.player.getX() / 8.0, mc.player.getZ() / 8.0)));
			} else if (dim == Level.NETHER) {
				lines.add(new Line("Overworld", String.format(Locale.ROOT, "%.1f / %.1f",
						mc.player.getX() * 8.0, mc.player.getZ() * 8.0)));
			}
		}
		if (cfg.showFacing) {
			lines.add(new Line("Kierunek", facingText(mc)));
		}
		return lines;
	}

	private static void drawPanel(GuiGraphics g, Minecraft mc, List<Line> lines, HudConfig cfg) {
		var font = mc.font;
		int pad = 4;
		int lineH = font.lineHeight + 2;

		int maxW = 0;
		for (Line l : lines) {
			int w = font.width(l.label() + ": ") + font.width(l.value());
			maxW = Math.max(maxW, w);
		}

		int boxW = maxW + pad * 2 + 3;
		int boxH = lines.size() * lineH + pad * 2 - 2;
		int margin = 6;

		int sw = g.guiWidth();
		int sh = g.guiHeight();
		int x = (cfg.corner == 1 || cfg.corner == 3) ? sw - boxW - margin : margin;
		int y = (cfg.corner == 2 || cfg.corner == 3) ? sh - boxH - margin : margin;

		if (cfg.showBackground) {
			g.fill(x, y, x + boxW, y + boxH, PANEL_BG);
			g.fill(x, y, x + 2, y + boxH, ACCENT);
		}

		int tx = x + pad + 3;
		int ty = y + pad;
		for (Line l : lines) {
			String label = l.label() + ": ";
			g.drawString(font, label, tx, ty, LABEL);
			g.drawString(font, l.value(), tx + font.width(label), ty, VALUE);
			ty += lineH;
		}
	}

	private static String serverName(Minecraft mc) {
		ServerData server = mc.getCurrentServer();
		if (server != null) {
			return server.ip;
		}
		return mc.hasSingleplayerServer() ? "Singleplayer" : "-";
	}

	private static String pingText(Minecraft mc) {
		if (mc.getConnection() == null || mc.hasSingleplayerServer()) {
			return "-";
		}
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		if (info == null) {
			return "-";
		}
		return info.getLatency() + " ms";
	}

	private static String facingText(Minecraft mc) {
		Direction dir = mc.player.getDirection();
		String name = switch (dir) {
			case NORTH -> "Polnoc (-Z)";
			case SOUTH -> "Poludnie (+Z)";
			case EAST -> "Wschod (+X)";
			case WEST -> "Zachod (-X)";
			default -> dir.getName();
		};
		float yaw = Mth.wrapDegrees(mc.player.getYRot());
		return name + String.format(Locale.ROOT, "  [%.0f]", yaw);
	}

	// --- CPS ---

	private static void updateCps(Minecraft mc) {
		long now = System.currentTimeMillis();
		long window = mc.getWindow().getWindow();

		boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

		if (mc.screen == null) {
			if (left && !leftWasDown) {
				LEFT_CLICKS.addLast(now);
			}
			if (right && !rightWasDown) {
				RIGHT_CLICKS.addLast(now);
			}
		}
		leftWasDown = left;
		rightWasDown = right;

		prune(LEFT_CLICKS, now);
		prune(RIGHT_CLICKS, now);
	}

	private static void prune(ArrayDeque<Long> q, long now) {
		while (!q.isEmpty() && now - q.peekFirst() > 1000) {
			q.pollFirst();
		}
	}

	private static int cps(ArrayDeque<Long> q) {
		return q.size();
	}
}
