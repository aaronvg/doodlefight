package screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GLCommon;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import com.komodo.doodlefight.Assets;
import com.komodo.doodlefight.Settings;

public class MainMenuScreen implements Screen {
	Game game;

	OrthographicCamera guiCam;
	SpriteBatch batcher;
	Rectangle soundBounds;
	Rectangle playBounds;
	Rectangle highscoresBounds;
	Rectangle helpBounds;
	Vector3 touchPoint;
	
	float width = 480;
	float height = 320;

	public MainMenuScreen (Game game) {
		this.game = game;
	
		guiCam = new OrthographicCamera(width, height);
		guiCam.position.set(width / 2, height / 2, 0);
		batcher = new SpriteBatch();
		soundBounds = new Rectangle(0, 0, 64, 64);
		playBounds = new Rectangle(10 + 50, 200 + 18, 300, 36);
		highscoresBounds = new Rectangle(10 + 50, 200 - 18, 300, 36);
		helpBounds = new Rectangle(10 + 50, 200 - 18 - 36, 300, 36);
		touchPoint = new Vector3();
	}

	public void update () {
		if (Gdx.input.justTouched()) {
			guiCam.unproject(touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0));

			if (playBounds.contains(touchPoint.x, touchPoint.y)) {
				Assets.playSound(Assets.clickSound);
				game.setScreen(new LevelSelect(game));
				//game.setScreen(new GameScreen(game));
				return;
			}
			if (highscoresBounds.contains(touchPoint.x, touchPoint.y)) {
				Assets.playSound(Assets.clickSound);
				game.setScreen(new HighscoresScreen(game));
				return;
			}
			if (helpBounds.contains(touchPoint.x, touchPoint.y)) {
				Assets.playSound(Assets.clickSound);
				game.setScreen(new HelpScreen(game));
				return;
			}
			if (soundBounds.contains(touchPoint.x, touchPoint.y)) {
				Assets.playSound(Assets.clickSound);
				Settings.soundEnabled = !Settings.soundEnabled;
				if (Settings.soundEnabled)
					Assets.music.play();
				else
					Assets.music.pause();
			}
		}
	}

	long last = TimeUtils.nanoTime();

	public void draw () {
		GLCommon gl = Gdx.gl;
		gl.glClearColor(1, 0, 0, 1);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		guiCam.update();
		batcher.setProjectionMatrix(guiCam.combined);

		batcher.disableBlending();
		batcher.begin();
		batcher.draw(Assets.backgroundRegion, 0, 0, width, height);
		batcher.end();

		batcher.enableBlending();
		batcher.begin();
		batcher.draw(Assets.logo, 160 - 274 / 2, 480 - 10 - 142, 274, 142);
		batcher.draw(Assets.mainMenu, 10 + 50, 200 - 110 / 2, 300, 110);
		batcher.draw(Settings.soundEnabled ? Assets.soundOn : Assets.soundOff, 0, 0, 64, 64);
		batcher.end();

		if (TimeUtils.nanoTime() - last > 2000000000) {
		/*	Gdx.app.log("SuperJumper",
				"version: " + Gdx.app.getVersion() + ", memory: " + Gdx.app.getJavaHeap() + ", " + Gdx.app.getNativeHeap()
					+ ", native orientation:" + Gdx.input.getNativeOrientation() + ", orientation: " + Gdx.input.getRotation()
					+ ", accel: " + (int)Gdx.input.getAccelerometerX() + ", " + (int)Gdx.input.getAccelerometerY() + ", "
					+ (int)Gdx.input.getAccelerometerZ() + ", apr: " + (int)Gdx.input.getAzimuth() + ", " + (int)Gdx.input.getPitch()
					+ ", " + (int)Gdx.input.getRoll());*/
			last = TimeUtils.nanoTime();
		}
	}

	@Override
	public void render (float delta) {
		update();
		draw();
	}

	@Override
	public void resize (int width, int height) {
	}

	@Override
	public void show () {
	}

	@Override
	public void hide () {
	}

	@Override
	public void pause () {
		Settings.save();
	}

	@Override
	public void resume () {
	}

	@Override
	public void dispose () {
	}
}