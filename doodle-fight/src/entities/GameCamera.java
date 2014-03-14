package entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class GameCamera extends OrthographicCamera {

	float initialZoom;
	Vector3 startPos;
	float originalX;
	float originalY;
	boolean init;

	public GameCamera(float frustrumWidth, float frustrumHeight, TiledMap map) {
		super(frustrumWidth, frustrumHeight);
		originalX = frustrumWidth /2;
		originalY = frustrumHeight / 2 + 2.5f;
		startPos = new Vector3(originalX, originalY, 1);
		Gdx.app.log("cam", "HERES THE DAMN CAMERA");
		MapProperties prop = map.getProperties();

		int mapWidth = prop.get("width", Integer.class);
		int mapHeight = prop.get("height", Integer.class);
		int tilePixelWidth = prop.get("tilewidth", Integer.class);
		int tilePixelHeight = prop.get("tileheight", Integer.class);

		int mapPixelWidth = mapWidth * tilePixelWidth;
		int mapPixelHeight = mapHeight * tilePixelHeight;

		Vector3 pos = new Vector3(mapWidth - (frustrumWidth / 2), mapHeight, 0);
		// unproject(pos);
		init = true;
		

		this.initialZoom = this.zoom;
		this.zoom = 2f;
		position.x = pos.x; // start at the other end of the map.
		position.y = position.y * zoom;
		update();


	}

	public void init() {

	}

	public void update() {
		super.update();

	}

	public void moveToPlayer() {
		if(init) { // We only do this at the start of the game.
			//Gdx.app.log("cam", "HERES THE DAMN CAMERA2");
			float lerp = 0.95f;
			Vector3 pos = position;
			pos.x += (startPos.x - pos.x) * .01f;
			pos.y += (startPos.y - pos.y) * .01f;
	
			zoom += (initialZoom - zoom) * .01f;
		}

	}
	
	
	

}