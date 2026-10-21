package pl.darkhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia moda zapisywane w config/darkhud.json. */
public class HudConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("darkhud.json");

	public static HudConfig INSTANCE = new HudConfig();

	// Elementy HUD
	public boolean showCoords = true;
	public boolean showNether = true;
	public boolean showFps = true;
	public boolean showPing = true;
	public boolean showCps = true;
	public boolean showFacing = true;
	public boolean showServer = true;
	public boolean showNick = true;

	// Wyglad
	public boolean showBackground = true;
	/** 0 = lewy gorny, 1 = prawy gorny, 2 = lewy dolny, 3 = prawy dolny */
	public int corner = 0;

	// Klawisze (kody GLFW)
	public int guiKey = GLFW.GLFW_KEY_8;
	public int hideCoordsKey = GLFW.GLFW_KEY_6;

	/** Chwilowe ukrycie koordynatow klawiszem (nie zapisywane). */
	public transient boolean coordsHidden = false;

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE)) {
				HudConfig loaded = GSON.fromJson(r, HudConfig.class);
				if (loaded != null) {
					INSTANCE = loaded;
				}
			} catch (Exception e) {
				DarkHudClient.LOGGER.warn("Nie udalo sie wczytac configu, uzywam domyslnych", e);
			}
		}
		save();
	}

	public static void save() {
		try (Writer w = Files.newBufferedWriter(FILE)) {
			GSON.toJson(INSTANCE, w);
		} catch (IOException e) {
			DarkHudClient.LOGGER.warn("Nie udalo sie zapisac configu", e);
		}
	}

	public static void reset() {
		INSTANCE = new HudConfig();
		save();
	}
}
