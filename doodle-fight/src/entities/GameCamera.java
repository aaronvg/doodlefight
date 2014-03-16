package entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class GameCamera extends OrthographicCamera implements GestureListener {

	float initialZoom;
	Vector3 startPos;
	float originalX;
	float originalY;
	boolean init;

	float width;
	float height;

	float velX, velY;
	boolean flinging = false;
	float initialScale;

	public GestureDetector gestureDetector;

	public GameCamera(float frustrumWidth, float frustrumHeight, TiledMap map) {
		super(frustrumWidth, frustrumHeight);
		originalX = frustrumWidth / 2;
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

		width = frustrumWidth;
		height = frustrumHeight;

		this.initialZoom = this.zoom;
		this.zoom = 2f;
		position.x = pos.x; // start at the other end of the map.
		position.y = position.y * zoom;
		update();
		initialScale = 1f;
		gestureDetector = new GestureDetector(this);

	}

	public void init() {

	}
	Vector2 lastTouched = new Vector2(0, 0);

	public void update() {
		super.update();
		if(lastTouched != null)
		Gdx.app.log("lastTouch", "last " +  lastTouched.x + " " + lastTouched.y + " fling" + flinging);
		if (flinging && lastTouched.x > width/3) {
			velX *= 0.96f;
			velY *= 0.96f;
			position.add(-velX, velY, 0);
			if (Math.abs(velX) < 0.0001f)
				velX = 0;
			if (Math.abs(velY) < 0.0001f)
				velY = 0;
		}

	}

	public void moveToPlayer() {
		// if(init) { // We only do this at the start of the game.
		// //Gdx.app.log("cam", "HERES THE DAMN CAMERA2");
		// float lerp = 0.95f;
		// Vector3 pos = position;
		// pos.x += (startPos.x - pos.x) * .01f;
		// pos.y += (startPos.y - pos.y) * .01f;
		//
		// zoom += (initialZoom - zoom) * .01f;
		// }

	}
	
	
	public boolean isDrawingArea(float x, float y) {
		
		Vector3 v = new Vector3(x, y, 0);
		unproject(v);
		Gdx.app.log("GestureDetectorTest", "checking bounds " + v.x  + " " + v.y);
		lastTouched.x = v.x;
		lastTouched.y = v.y;
		if(v.x < width / 3)
			return true;
		return false;
		
	}
	

	boolean zooming = false;
	
	@Override
	public boolean touchDown(float x, float y, int pointer, int button) {
		if(isDrawingArea(x, y)) {
			flinging = false;
			return false;
		}
		
		flinging = false;
		initialScale = zoom;
		Gdx.app.log("GestureDetectorTest", "init scale" + initialScale
				+ " zoom " + zoom);

		return false;
	}

	@Override
	public boolean tap(float x, float y, int count, int button) {
		flinging =false;
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean longPress(float x, float y) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean fling(float velocityX, float velocityY, int button) {
		Gdx.app.log("GestureDetectorTest", "fling " + velocityX + ", "
				+ velocityY);
		flinging = true;
		
		velX = zoom * velocityX * 0.0001f;
		velY = zoom * velocityY * 0.0001f;

		return false;
	}

	boolean first = true;
	float unit = 1 / 70f; // pixels per unit in the world.

	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY) {
		if(isDrawingArea(x, y)) {
			flinging = false;
			return false;
		}
		
		
		
		Gdx.app.log("GestureDetectorTest", "pan at " + x + ", " + y + "speed "
				+ deltaX + " " + deltaY);
		// position.add(-deltaX * .01f * zoom, deltaY * .01f * zoom, 0);
		if (first) {
			first = false;
			return false;
		} else {
			position.x -= deltaX * unit * zoom;
			position.y += deltaY * unit * zoom;
		}
		return true;
	}

	@Override
	public boolean panStop(float x, float y, int pointer, int button) {
		// TODO Auto-generated method stub
		if(isDrawingArea(x, y))
			flinging = false;
		first = true;
		return true;
	}

	float prevZoom = 0f;
	boolean first2 = true;

	@Override
	public boolean zoom(float originalDistance, float currentDistance) {
		zooming = true;
		float ratio = originalDistance / currentDistance;
		zoom = initialScale * ratio;
		return false;
	}

	@Override
	public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2,
			Vector2 pointer1, Vector2 pointer2) {

		return false;
	}

}
