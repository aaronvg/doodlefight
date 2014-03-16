/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gameWorld;

import java.util.ArrayList;

import mdesl.swipe.SwipeHandler;
import mdesl.swipe.mesh.SwipeTriStrip;
import box2DLights.PointLight;
import box2DLights.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.Map;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.komodo.doodlefight.Assets;

import entities.DrawingManager;
import entities.GameCamera;

public class WorldRenderer {

	private BitmapFont bitmapFont;

	static final float FRUSTUM_WIDTH = 17;
	static final float FRUSTUM_HEIGHT = 10;

	GameWorld world;
	// OrthographicCamera cam;
	public static GameCamera cam;
	SpriteBatch batch;
	TextureRegion background;
	private OrthogonalTiledMapRenderer renderer;

	ShapeRenderer shapeRender;

	float dampingCounter;
	float followX;
	float followY;
	Vector2 lookAhead;
	Vector2 target;

	private Texture ghostTexture;
	private TextureRegion ghostRegion;
	private float originX;
	private float originY;
	private float textureWidth;
	private float textureHeight;

	Box2DDebugRenderer debugRenderer;
	// TODO attach light as a component of an object.
	RayHandler handler;
	RayHandler handler2;
	public TiledMap map; // this is what the worldRenderer uses.
	MapBodyBuilder mapBuilder;

	PointLight pointLight;
	PointLight pointLight2;

	private int lightSize = 64;
	// private int lightSize = 160;
	Vector2 velocity;

	private float upScale = 1.5f; // for example; try lightSize=128,
									// upScale=1.5f

	BitmapFont font;

	TextureRegion shadowMap1D; // 1 dimensional shadow map
	TextureRegion occluders; // occluder map

	FrameBuffer shadowMapFBO;
	FrameBuffer occludersFBO;

	Texture casterSprites;
	TextureRegion casterRegion;
	// Texture light;

	ShaderProgram shadowMapShader, shadowRenderShader;

	boolean additive = true;
	boolean softShadows = true;

	Vector3 pos;

	public final static short FILTER_CATEGORY_SCENERY = 0x0001;
	public final static short FILTER_CATEGORY_LIGHT = 0x0002; // LIGHT PASS
																// THROUGH
	public static final short FILTER_CATEGORY_DONT_ABSORB_LIGHT = 0x0004;
	public static final short FILTER_CATEGORY_LIGHT_PASS = 0x0008;
	public final static short FILTER_MASK_SCENERY = -1;
	public final static short FILTER_MASK_DONT_ABSORB_LIGHT = FILTER_CATEGORY_SCENERY
			| FILTER_CATEGORY_LIGHT;
	public final static short FILTER_MASK_ABSORB_LIGHT = FILTER_CATEGORY_SCENERY;
	DrawingManager drawingManager;

	public WorldRenderer(SpriteBatch batch, GameWorld world) {
		this.world = world;
		this.cam = new GameCamera(FRUSTUM_WIDTH, FRUSTUM_HEIGHT, world.map);
		// this.cam = new OrthographicCamera(FRUSTUM_WIDTH, FRUSTUM_HEIGHT);
		// this.cam.position.set(FRUSTUM_WIDTH / 2, FRUSTUM_HEIGHT / 2, 0);
		this.batch = batch;
		renderer = new OrthogonalTiledMapRenderer(world.map, 1 / 70f); // the 70
																		// is
																		// because
																		// each
																		// tile
																		// is
																		// 70px
		followX = 0f;
		followY = 0f;
		shapeRender = new ShapeRenderer();

		ghostTexture = new Texture("data/ghost_fixed.png");
		ghostRegion = new TextureRegion(ghostTexture, 0, 0, 64, 64);
		originX = 1 / 48f * ghostTexture.getWidth() / 2f;
		originY = 1 / 48f * ghostTexture.getHeight() / 2f;
		textureWidth = 1 / 48f * 64; // texture is 64 pixels big.
		textureHeight = 1 / 48f * 64;

		dampingCounter = 0;
		velocity = new Vector2();

		// Light setup ---------------------------
		RayHandler.useDiffuseLight(true);
		handler = new RayHandler(world.world2);
		// handler.setAmbientLight(.1f, .1f, .1f, 1f);
		handler.setAmbientLight(.8f, .8f, .8f, 1f);
		// handler.setAmbientLight(.06f, .06f, .06f, .05f);
		handler.setShadows(true);
		handler.setCulling(true);
		// handler.setBlur(true);

		// Renders shadows
		pointLight = new PointLight(handler, 290, new Color(1, 1, .8f, .6f),
				15, 10, 110);
		pointLight.setSoft(true);
		pointLight.setSoftnessLenght(.3f);

		// lights up the tiles around the player (no shadows)
		/*pointLight2 = new PointLight(handler, 200, new Color(1, 1, .8f, .6f),
				6, 10, 110);
		pointLight2.setSoftnessLenght(1.5f);
		pointLight2.setXray(true);
		*/
		// for debugging---------
		// Box2D debug renderer. renders wireframes of the objects we create.
		debugRenderer = new Box2DDebugRenderer();
		bitmapFont = new BitmapFont();
		bitmapFont.setUseIntegerPositions(false);
		bitmapFont.getRegion().getTexture()
				.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		bitmapFont.setColor(Color.WHITE);
		bitmapFont.setScale(1.0f / 48.0f);

		startSwipe();

	}

	SwipeHandler swipe;

	Texture tex;
	ShapeRenderer shapes;

	SwipeTriStrip tris;

	public void startSwipe() {
		// the triangle strip renderer
		tris = new SwipeTriStrip();

		// a swipe handler with max # of input points to be kept alive
		swipe = new SwipeHandler(10);

		// minimum distance between two points
		swipe.minDistance = 50; // 10

		// minimum distance between first and second point
		swipe.initialDistance = 50; // 10

		// we will use a texture for the smooth edge, and also for stroke
		// effects
		tex = new Texture("data/gradient.png");
		tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

		shapes = new ShapeRenderer();

		// handle swipe input
		Gdx.input.setInputProcessor(swipe);
		drawingManager = new DrawingManager(swipe);
	}

	public void render() {
		// Set lights
		pointLight.setPosition(world.bob.position.x, world.bob.position.y);
		// pointLight2.setPosition(world.bob.position.x, world.bob.position.y);
		// handler.setCombinedMatrix(cam.combined, cam.position.x,
		// cam.position.y,
		// cam.viewportWidth * cam.zoom, cam.viewportHeight * cam.zoom);

		cam.moveToPlayer();
		cam.update();

		renderBackground();
		renderObjects();
		swipeDraw();

	}

	void swipeDraw() {

		// the endcap scale
		tris.endcap = .4f;

		// the thickness of the line
		tris.thickness = .5f;

		// generate the triangle strip from our path
		tris.update(swipe.path());

		// the vertex color for tinting, i.e. for opacity
		tris.color = Color.BLACK;

		// render the triangles to the screen
		// tris.draw(cam);

		// uncomment to see debug lines
		drawDebug();

		drawingManager.update(cam, batch);
	}

	ArrayList<Vector2> outline = new ArrayList<Vector2>();
	// optional debug drawing..
	void drawDebug() {
		Array<Vector2> input = swipe.input();

		// draw the raw input
		shapes.begin(ShapeType.Line);
		shapes.setColor(Color.GRAY);
		for (int i = 0; i < input.size - 1; i++) {
			Vector2 p = input.get(i);
			Vector2 p2 = input.get(i + 1);
			shapes.line(p.x, p.y, p2.x, p2.y);
			// Gdx.app.log("point", "real " + p.y);
		}
		shapes.end();

		// draw the smoothed and simplified path
		shapes.begin(ShapeType.Line);
		shapes.setColor(Color.RED);
		Array<Vector2> out = swipe.path();
		for (int i = 0; i < out.size - 1; i++) {
			Vector2 p = out.get(i);
			Vector2 p2 = out.get(i + 1);
			shapes.line(p.x, p.y, p2.x, p2.y);
		}
		shapes.end();

		shapes.begin(ShapeType.Line);
		Vector2 perp = new Vector2();

		for (int i = 0; i < input.size - 1; i++) {
			Vector2 p = input.get(i);
			Vector2 p2 = input.get(i + 1);

			shapes.setColor(Color.LIGHT_GRAY);
			perp.set(p).sub(p2).nor();
			perp.set(perp.y, -perp.x);
			perp.scl(10f);
			shapes.line(p.x, p.y, p.x + perp.x, p.y + perp.y);

			perp.scl(-1f);

			shapes.setColor(Color.BLUE);
			shapes.line(p.x, p.y, p.x + perp.x, p.y + perp.y); // p.x + perp.x
																// is the point
																// we want.
																// (bottom)
		}
		shapes.end();

		// calculate the outline
		ArrayList<Vector2> temp;
		temp = drawingManager.getNextArrow();
		if(temp.size() > 0) {
			outline = temp;
			world.createArrow(outline);
		}
/*
		for (int i = 0; i < input.size - 1; i++) {
			if (i < input.size - 1) {
				Vector2 p = input.get(i);
				Vector2 p2 = input.get(i + 1);

				perp.set(p).sub(p2).nor();
				perp.set(perp.y, -perp.x);
				perp.scl(10f);

				Vector2 perpVector = new Vector2(p.x + perp.x, p.y + perp.y);
				outline.add(perpVector);
			}
		}
		for (int i = input.size - 1 -  1; i >= 0; i--) {
			if(i == input.size - 1 - 1) {
				Vector2 a = input.get(input.size - 1);
				outline.add(a); // this would be the arrowhead.
			}
			Vector2 p = input.get(i);
			Vector2 p2 = input.get(i + 1);

			perp.set(p).sub(p2).nor();
			perp.set(perp.y, -perp.x);
			perp.scl(10f);

			perp.scl(-1f);
			Vector2 perpVector = new Vector2(p.x + perp.x, p.y + perp.y);
			outline.add(perpVector);
		}
	*/
		shapes.setProjectionMatrix(cam.combined); // set when drawing next arrow only
		shapes.begin(ShapeType.Filled);
		float red = .1f;
		for (int i = 0; i < outline.size(); i++) {
			Vector2 p = outline.get(i);
			red += .05f;
			if (red > 1.0)
				red = 1.0f;
			if(i == outline.size()/2)
			{
				shapes.setColor(Color.BLUE);
			}
			else
				shapes.setColor(red, 0, 0, 1);
			shapes.circle(p.x, p.y, .2f, 10);

		}
		shapes.end();
	}

	public void renderBackground() {
	}

	public void renderObjects() {

		// Draw all tiles now and black background
		shapeRender.begin(ShapeType.Filled);
		shapeRender.setColor(new Color(.1f, .1f, .1f, 1f));
		shapeRender
				.rect(cam.position.x - 200, cam.position.y - 200, 2580, 1680);
		shapeRender.end();
		batch = (SpriteBatch) renderer.getSpriteBatch();
		renderer.setView(cam.combined, cam.position.x - (100), cam.position.y
				- (20 * cam.zoom), cam.viewportWidth + 100, cam.viewportWidth
				+ 40 * cam.zoom);
		renderer.render();

		// render game objects
		batch.begin();
		renderBob();
		renderPlatforms();
		// renderItems();
		// renderCastle();
		batch.end();

		// Debug rendering
		// draw fonts
		debugRender();
		// draw box2d world.
		debugRenderer.render(world.world2, cam.combined);

		// light rendering
		// handler.updateAndRender();
	}

	public void debugRender() {
		batch.begin();
		float x = cam.position.x;
		/*bitmapFont.setColor(shaderSelection==ShaderSelection.Default?Color.YELLOW:Color.WHITE);
		x += bitmapFont.draw(batch, "1=Default Shader", x, cam.position.y).width;
		bitmapFont.setColor(shaderSelection==ShaderSelection.Ambiant?Color.YELLOW:Color.WHITE);
		x += bitmapFont.draw(batch, " 2=Ambient Light", x, cam.position.y).width;
		bitmapFont.setColor(shaderSelection==ShaderSelection.Light?Color.YELLOW:Color.WHITE);
		x += bitmapFont.draw(batch, " 3=Light Shader", x, cam.position.y).width;
		bitmapFont.setColor(shaderSelection==ShaderSelection.Final?Color.YELLOW:Color.WHITE);
		x += bitmapFont.draw(batch, " 4=Final Shader", x, cam.position.y).width;
		x = cam.position.x;
		bitmapFont.setColor(lightMove?Color.YELLOW:Color.WHITE);
		x += bitmapFont.draw(batch, "click=light control (" +lightMove+ ")", x, cam.position.y-bitmapFont.getLineHeight()).width;
		bitmapFont.setColor(lightOscillate?Color.YELLOW:Color.WHITE);
		x += bitmapFont.draw(batch, " space=light flicker (" +lightOscillate+ ")", x, cam.position.y-bitmapFont.getLineHeight()).width;
		x = cam.position.x;
		bitmapFont.setColor(Color.WHITE);*/
		x += bitmapFont.draw(batch, Gdx.graphics.getFramesPerSecond() + " fps",
				x, cam.position.y - bitmapFont.getLineHeight() * 2.0f).width;
		batch.end();
	}

	private void renderBob() {
		/*TextureRegion keyFrame;
		switch (world.bob.state) {
		case Bob.BOB_STATE_FALL:
			keyFrame = Assets.bobFall.getKeyFrame(world.bob.stateTime);
			break;
		case Bob.BOB_STATE_JUMP:
			keyFrame = Assets.bobJump.getKeyFrame(world.bob.stateTime);
			break;
		case Bob.BOB_STATE_HIT:
		default:
			keyFrame = Assets.bobHit;
		}*/

		batch.draw(ghostRegion, world.bob.position.x - .7f,
				world.bob.position.y - .64f, originX, originY, textureWidth,
				textureHeight, 1, 1, 0, false);

		batch.draw(ghostRegion, world.bob.position2.x - .2f,
				world.bob.position2.y - 1.5f, originX, originY,
				textureWidth * .5f, textureHeight * .5f, 1, 1,
				-world.bob.angleBaby + 180, false);

		// batch.draw(ghostRegion, world.bob.position.x - .2f,
		// world.bob.position.y - .2f, originX, originY, textureWidth,
		// textureHeight, 1, 1, world.sensor.getAzimuth(), false);

		/*	float side = world.bob.velocity.x < 0 ? -1 : 1;
			if (side < 0) { // the -1 is to render a bit higher than usual {
				// batch.draw(keyFrame, world.bob.position.x + 0.5f + .4f,
				// world.bob.position.y - 0.5f + .5f, side * 1, 1);

				batch.draw(keyFrame, world.bob.position.x, world.bob.position.y,
						.5f, .5f, (float) keyFrame.getTexture().getWidth(),
						(float) keyFrame.getTexture().getWidth(), 1f, 1f,
						world.sensor.getAzimuth(), false);
			}

			else {
				// batch.draw(keyFrame, world.bob.position.x - 0.5f +.4f,
				// world.bob.position.y - 0.5f + .5f, side * 1, 1);
				batch.draw(keyFrame, world.bob.position.x, world.bob.position.y,
						.5f, .5f, (float) keyFrame.getTexture().getWidth(),
						(float) keyFrame.getTexture().getWidth(), 1f, 1f,
						world.sensor.getAzimuth(), false);
			}*/
	}

	private void renderPlatforms() {
		// int len = world.platforms.size();
		// for (int i = 0; i < len; i++) {
		// Platform platform = world.platforms.get(i);
		// TextureRegion keyFrame = Assets.platform;
		// if (platform.state == Platform.PLATFORM_STATE_PULVERIZING) {
		// keyFrame = Assets.brakingPlatform.getKeyFrame(platform.stateTime,
		// Animation.ANIMATION_NONLOOPING);
		// }
		//
		// batch.draw(keyFrame, platform.position.x - 1, platform.position.y -
		// 0.25f, 2, 0.5f);
		// }
	}

	private void renderItems() {
		if (world.collision) {
			Coin coin = new Coin(world.bob.position.x, world.bob.position.y);
			TextureRegion keyFrame = Assets.coinAnim.getKeyFrame(
					coin.stateTime, true);
			batch.draw(keyFrame, coin.position.x - 0.5f,
					coin.position.y - 0.5f, 1, 1);
		}
		// int len = world.springs.size();
		// for (int i = 0; i < len; i++) {
		// Spring spring = world.springs.get(i);
		// batch.draw(Assets.spring, spring.position.x - 0.5f, spring.position.y
		// - 0.5f, 1, 1);
		// }
		//
		// len = world.coins.size();
		// for (int i = 0; i < len; i++) {
		// Coin coin = world.coins.get(i);
		// TextureRegion keyFrame = Assets.coinAnim.getKeyFrame(coin.stateTime,
		// Animation.ANIMATION_LOOPING);
		// batch.draw(keyFrame, coin.position.x - 0.5f, coin.position.y - 0.5f,
		// 1, 1);
		// }
	}

	private void renderCastle() {
		// Castle castle = world.castle;
		// batch.draw(Assets.castle, castle.position.x - 1, castle.position.y -
		// 1, 2, 2);
	}

	private class MapBodyBuilder {

		// The pixels per tile. If your tiles are 16x16, this is set to 16f
		private float ppt = 0;

		public Array<Body> buildShapes(Map map, float pixels, World world) {
			ppt = pixels;
			MapObjects objects = map.getLayers().get("Obstacles").getObjects();

			Array<Body> bodies = new Array<Body>();

			for (MapObject object : objects) {

				if (object instanceof TextureMapObject) {
					continue;
				}

				Shape shape;

				if (object instanceof RectangleMapObject) {
					shape = getRectangle((RectangleMapObject) object);
				} else if (object instanceof PolygonMapObject) {
					shape = getPolygon((PolygonMapObject) object);
				} else if (object instanceof PolylineMapObject) {
					shape = getPolyline((PolylineMapObject) object);
				} else if (object instanceof CircleMapObject) {
					shape = getCircle((CircleMapObject) object);
				} else {
					continue;
				}

				BodyDef bd = new BodyDef();

				//
				// groundBodyDef.position.set(5,84);

				bd.type = BodyType.KinematicBody;
				// bd.type = BodyType.DynamicBody;
				Body body = world.createBody(bd);
				body.createFixture(shape, 1);

				bodies.add(body);

				shape.dispose();
			}
			return bodies;
		}

		public PolygonShape getRectangle(RectangleMapObject rectangleObject) {
			Rectangle rectangle = rectangleObject.getRectangle();
			PolygonShape polygon = new PolygonShape();
			Vector2 size = new Vector2((rectangle.x + rectangle.width * 0.5f)
					/ ppt, (rectangle.y + rectangle.height * 0.5f) / ppt);
			polygon.setAsBox(rectangle.width * 0.5f / ppt, rectangle.height
					* 0.5f / ppt, size, 0.0f);
			return polygon;
		}

		private CircleShape getCircle(CircleMapObject circleObject) {
			Circle circle = circleObject.getCircle();
			CircleShape circleShape = new CircleShape();
			circleShape.setRadius(circle.radius / ppt);
			circleShape
					.setPosition(new Vector2(circle.x / ppt, circle.y / ppt));
			return circleShape;
		}

		private PolygonShape getPolygon(PolygonMapObject polygonObject) {
			PolygonShape polygon = new PolygonShape();
			float[] vertices = polygonObject.getPolygon()
					.getTransformedVertices();

			float[] worldVertices = new float[vertices.length];

			for (int i = 0; i < vertices.length; ++i) {
				System.out.println(vertices[i]);
				worldVertices[i] = vertices[i] / ppt;
			}

			polygon.set(worldVertices);
			return polygon;
		}

		private ChainShape getPolyline(PolylineMapObject polylineObject) {
			float[] vertices = polylineObject.getPolyline()
					.getTransformedVertices();
			Vector2[] worldVertices = new Vector2[vertices.length / 2];

			for (int i = 0; i < vertices.length / 2; ++i) {
				worldVertices[i] = new Vector2();
				worldVertices[i].x = vertices[i * 2] / ppt;
				worldVertices[i].y = vertices[i * 2 + 1] / ppt;
			}

			ChainShape chain = new ChainShape();
			chain.createChain(worldVertices);
			return chain;
		}
	}

}
