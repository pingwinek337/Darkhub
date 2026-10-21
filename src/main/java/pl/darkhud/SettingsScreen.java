package pl.darkhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Ciemne GUI ustawien: przelaczniki HUD, zmiana klawiszy, zapis i reset. */
public class SettingsScreen extends Screen {
	private static final int BG_OVERLAY = 0xB0000000;
	private static final int PANEL = 0xF0141419;
	private static final int PANEL_BORDER = 0xFF2A2A35;
	private static final int BTN = 0xFF1E1E26;
	private static final int BTN_HOVER = 0xFF2B2B38;
	private static final int ON = 0xFF8B7CFF;
	private static final int TEXT = 0xFFF2F2F7;
	private static final int MUTED = 0xFF9A9AB0;

	private static final int PANEL_W = 320;
	private static final int ROW_H = 20;
	private static final int GAP = 4;

	private static class Btn {
		int x, y, w, h;
		Supplier<String> label;
		Supplier<Boolean> active;
		Runnable action;

		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private final List<Btn> buttons = new ArrayList<>();
	private int panelX, panelY, panelH;

	/** 0 = nic, 1 = czekamy na klawisz GUI, 2 = czekamy na klawisz ukrywania */
	private int listening = 0;
	private boolean guiKeyWasDown = true;

	public SettingsScreen() {
		super(Component.literal("DarkHUD"));
	}

	@Override
	protected void init() {
		buttons.clear();
		HudConfig cfg = HudConfig.INSTANCE;

		int rows = 0;
		// 8 elementow HUD w dwoch kolumnach = 4 wiersze
		// + tlo, pozycja = 1 wiersz
		// + 2 klawisze = 2 wiersze
		// + zapisz/reset = 1 wiersz
		rows = 4 + 1 + 2 + 1;
		panelH = 40 + rows * (ROW_H + GAP) + 24;
		panelX = (this.width - PANEL_W) / 2;
		panelY = Math.max(4, (this.height - panelH) / 2);

		int innerX = panelX + 12;
		int innerW = PANEL_W - 24;
		int colW = (innerW - GAP) / 2;
		int y = panelY + 36;

		// Przelaczniki elementow HUD
		addToggle(innerX, y, colW, "Coords", () -> cfg.showCoords, () -> cfg.showCoords = !cfg.showCoords);
		addToggle(innerX + colW + GAP, y, colW, "Nether", () -> cfg.showNether, () -> cfg.showNether = !cfg.showNether);
		y += ROW_H + GAP;
		addToggle(innerX, y, colW, "FPS", () -> cfg.showFps, () -> cfg.showFps = !cfg.showFps);
		addToggle(innerX + colW + GAP, y, colW, "Ping", () -> cfg.showPing, () -> cfg.showPing = !cfg.showPing);
		y += ROW_H + GAP;
		addToggle(innerX, y, colW, "CPS", () -> cfg.showCps, () -> cfg.showCps = !cfg.showCps);
		addToggle(innerX + colW + GAP, y, colW, "Facing", () -> cfg.showFacing, () -> cfg.showFacing = !cfg.showFacing);
		y += ROW_H + GAP;
		addToggle(innerX, y, colW, "Server", () -> cfg.showServer, () -> cfg.showServer = !cfg.showServer);
		addToggle(innerX + colW + GAP, y, colW, "Nick", () -> cfg.showNick, () -> cfg.showNick = !cfg.showNick);
		y += ROW_H + GAP;

		// Wyglad
		addToggle(innerX, y, colW, "Tlo panelu", () -> cfg.showBackground, () -> cfg.showBackground = !cfg.showBackground);
		addButton(innerX + colW + GAP, y, colW, () -> "Pozycja: " + cornerName(cfg.corner), () -> false,
				() -> cfg.corner = (cfg.corner + 1) % 4);
		y += ROW_H + GAP;

		// Klawisze
		addButton(innerX, y, innerW, () -> listening == 1
						? "Klawisz GUI: > nacisnij klawisz <"
						: "Klawisz GUI: " + keyName(cfg.guiKey),
				() -> listening == 1, () -> listening = (listening == 1) ? 0 : 1);
		y += ROW_H + GAP;
		addButton(innerX, y, innerW, () -> listening == 2
						? "Ukryj koordy: > nacisnij klawisz <"
						: "Ukryj koordy: " + keyName(cfg.hideCoordsKey),
				() -> listening == 2, () -> listening = (listening == 2) ? 0 : 2);
		y += ROW_H + GAP;

		// Zapisz / reset
		addButton(innerX, y, colW, () -> "Zapisz i zamknij", () -> false, () -> {
			HudConfig.save();
			onClose();
		});
		addButton(innerX + colW + GAP, y, colW, () -> "Resetuj", () -> false, () -> {
			HudConfig.reset();
			listening = 0;
			init();
		});
	}

	private void addToggle(int x, int y, int w, String name, Supplier<Boolean> state, Runnable toggle) {
		addButton(x, y, w, () -> name + ": " + (state.get() ? "ON" : "OFF"), state, toggle);
	}

	private void addButton(int x, int y, int w, Supplier<String> label, Supplier<Boolean> active, Runnable action) {
		Btn b = new Btn();
		b.x = x;
		b.y = y;
		b.w = w;
		b.h = ROW_H;
		b.label = label;
		b.active = active;
		b.action = action;
		buttons.add(b);
	}

	private static String cornerName(int c) {
		return switch (c) {
			case 1 -> "prawy gora";
			case 2 -> "lewy dol";
			case 3 -> "prawy dol";
			default -> "lewy gora";
		};
	}

	public static String keyName(int code) {
		if (code < 0) {
			return "brak";
		}
		try {
			return InputConstants.getKey(code, -1).getDisplayName().getString();
		} catch (Exception e) {
			return "KEY " + code;
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, BG_OVERLAY);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);

		// Panel
		g.fill(panelX - 1, panelY - 1, panelX + PANEL_W + 1, panelY + panelH + 1, PANEL_BORDER);
		g.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, PANEL);
		g.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, ON);

		g.drawString(this.font, "DarkHUD", panelX + 12, panelY + 12, TEXT);
		String hint = "ESC - zamknij";
		g.drawString(this.font, hint, panelX + PANEL_W - 12 - this.font.width(hint), panelY + 12, MUTED);

		for (Btn b : buttons) {
			boolean hover = b.contains(mouseX, mouseY);
			boolean on = b.active.get();
			g.fill(b.x, b.y, b.x + b.w, b.y + b.h, hover ? BTN_HOVER : BTN);
			if (on) {
				g.fill(b.x, b.y, b.x + 2, b.y + b.h, ON);
			}
			String text = b.label.get();
			int tx = b.x + (b.w - this.font.width(text)) / 2;
			int ty = b.y + (b.h - this.font.lineHeight) / 2 + 1;
			g.drawString(this.font, text, tx, ty, on ? TEXT : MUTED);
		}

		String foot = "Ustawienia zapisuja sie do config/darkhud.json";
		g.drawString(this.font, foot, panelX + (PANEL_W - this.font.width(foot)) / 2,
				panelY + panelH - 14, MUTED);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0) {
			for (Btn b : buttons) {
				if (b.contains(mx, my)) {
					b.action.run();
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		if (listening != 0) {
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				listening = 0;
				return true;
			}
			HudConfig cfg = HudConfig.INSTANCE;
			if (listening == 1) {
				cfg.guiKey = key;
				// zeby nie zamknac okna od razu tym samym klawiszem
				guiKeyWasDown = true;
			} else {
				cfg.hideCoordsKey = key;
			}
			listening = 0;
			HudConfig.save();
			return true;
		}
		return super.keyPressed(key, scancode, mods);
	}

	@Override
	public void tick() {
		// Klawisz GUI zamyka okno (z wykrywaniem zbocza, bo klawisz otwierajacy jest jeszcze wcisniety)
		if (this.minecraft == null) {
			return;
		}
		boolean down = DarkHudClient.isDown(this.minecraft, HudConfig.INSTANCE.guiKey);
		if (listening == 0 && down && !guiKeyWasDown) {
			onClose();
			return;
		}
		guiKeyWasDown = down;
	}

	@Override
	public void onClose() {
		HudConfig.save();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
