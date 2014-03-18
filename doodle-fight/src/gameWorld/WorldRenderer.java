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

import entities.DrawingManager;
import entities.GameCamera;
import entities.Light;

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

	Vector2 velocity;

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
	Light light;

	public WorldRenderer(SpriteBatch batch, GameWorld world) {
		this.world = world;
		this.cam = new GameCamera(FRUSTUM_WIDTH, FRUSTUM_HEIGHT, world.map);
		this.batch = batch;
		renderer = new OrthogonalTiledMapRenderer(world.map, 1 / 70f); // each
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
		
		//---- input ---
		world.multiplexer.addProcessor(cam.gestureDetector);
		world.multiplexer.getProcessors().reverse();
		Gdx.input.setInputProcessor(world.multiplexer);
		

		// Light setup ---------------------------
		light = new Light(world.world2);

		// for debugging---------
		debugRenderer = new Box2DDebugRenderer();
		bitmapFont = new BitmapFont();
		bitmapFont.setUseIntegerPositions(false);
		bitmapFont.getRegion().getTexture()
				.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		bitmapFont.setColor(Color.WHITE);
		bitmapFont.setScale(1.0f / 48.0f);
	}

	public void render() {

		cam.moveToPlayer();
		cam.update();
		renderBackground();

		renderObjects();
		drawBounds();
		light.draw(cam);
	}

	public void renderBackground() {
		// Draw all tiles now and black background
		shapeRender.begin(ShapeType.Filled);
		shapeRender.setColor(new Color(.1f, .1f, .1f, 1f));
		shapeRender
				.rect(cam.position.x - 200, cam.position.y - 200, 2580, 1680);
		shapeRender.end();
		batch = (SpriteBatch) renderer.getSpriteBatch();
		renderer.setView(cam.combined, cam.position.x - (100), cam.position.y
				- (100), cam.viewportWidth + 100, cam.viewportWidth
				+ 100);

		renderer.render();

	}

	public void renderObjects() {

		// Debug rendering
		// draw fonts
		debugRender();
		debugRenderer.render(world.world2, cam.combined); // this changes some colors...

		// TODO only create one
		// immediatemoderenderer...
		for (Arrow b : world.arrows) {
			b.drawStrip(cam);
		}
		world.drawingManager.update(cam); // draws current line.

		batch.begin();
		for (Arrow a : world.arrows) {
			a.draw(cam, batch);
		}
		renderBob();
		batch.end();
	}

	
	public void drawBounds() {
		
		Vector2 start = new Vector2(FRUSTUM_WIDTH / 3, 0); //10 is the drawbound.
		Vector2 end = new Vector2(FRUSTUM_WIDTH / 3, 20);
		
		shapeRender.begin(ShapeType.Line);
		shapeRender.setColor(Color.GRAY);
		shapeRender.setProjectionMatrix(cam.combined);
		shapeRender.line(start, end  );
		shapeRender.end();
	}
	
	
	public void debugRender() {
		batch.begin();
		float x = cam.position.x;
		/*
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
		batch.draw(ghostRegion, world.bob.position.x - .7f,
				world.bob.position.y - .64f, originX, originY, textureWidth,
				textureHeight, 1, 1, 0, false);

		batch.draw(ghostRegion, world.bob.position2.x - .2f,
				world.bob.position2.y - 1.5f, originX, originY,
				textureWidth * .5f, textureHeight * .5f, 1, 1,
				-world.bob.angleBaby + 180, false);
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
