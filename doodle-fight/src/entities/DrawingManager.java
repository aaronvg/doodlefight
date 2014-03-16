package entities;

import java.util.ArrayList;

import mdesl.swipe.SwipeHandler;
import mdesl.swipe.mesh.SwipeTriStrip;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * This class controls all drawing to the game. It helps with the conversion of paths to box2D
 * objects
 * 
 * @author Aaron
 * 
 */
public class DrawingManager {
	ArrayList<Vector2> currPoints;
	ArrayList<Line> paths;
	ShapeRenderer shaper;
	ArrayList<Vector2> smoothLine;
	ArrayList<Vector2> smoothLine2;
	boolean newPath;
	ImmediateModeRenderer20 gl20;

	Array<Vector2> texcoord = new Array<Vector2>();
	Array<Vector2> tristrip = new Array<Vector2>();
	ArrayList<Strip> strips = new ArrayList<Strip>();
	ArrayList<ArrowData> arrows = new ArrayList<ArrowData>();
	int batchSize;
	Vector2 perp = new Vector2();
	public float thickness = .2f;
	public float endcap = 0f;
	public Color color;

	SwipeTriStrip tris;
	int b;
	boolean newArrow = false;
	SwipeHandler swipe;
	Texture tex;

	// TODO: add drawing bounds for this manager.
	public DrawingManager(InputMultiplexer multiplexer) {
		// the triangle strip renderer
		tris = new SwipeTriStrip();
		tris.endcap = .0f;
		tris.thickness = .1f;
		// a swipe handler with max # of input points to be kept alive
		swipe = new SwipeHandler(10);
		swipe.minDistance = 50; // 10
		swipe.initialDistance = 50; // 10
		multiplexer.addProcessor(swipe);
		
		//Gdx.input.setInputProcessor(swipe);
		// ----------------------------------------------------------

		gl20 = new ImmediateModeRenderer20(false, true, 1);
		newPath = true;
		currPoints = new ArrayList<Vector2>();
		paths = new ArrayList<Line>();

		shaper = new ShapeRenderer();
		shaper.setColor(Color.BLACK);

		smoothLine = new ArrayList<Vector2>();
		smoothLine2 = new ArrayList<Vector2>();
		arrows = new ArrayList<ArrowData>();

		color = new Color(0.63f, 0.51f, 0.40f, 1);
	}

	public void update(GameCamera cam) {
		tris.update(swipe.path());

		// If input is touched, we're drawing the current path.
		if (Gdx.input.isTouched()
				&& (Gdx.input.getX() < Gdx.graphics.getWidth() / 3)) {
			newArrow = true;
			// Start drawing path. save it.
			currPoints = swipe.input2();
			for (int i = 0; i < currPoints.size(); i++) {
				Vector3 point = new Vector3(currPoints.get(i).x,
						currPoints.get(i).y, 1);
				cam.unproject(point);
				currPoints.set(i, new Vector2(point.x, point.y));
			}
		}

		tristrip.clear();
		texcoord.clear();

		drawCurrentLine(cam);

		// Activated only once per drawing. (not touched, and theres an arrow
		// waiting in the buffer)
		if (!Gdx.input.isTouched() && newArrow == true
				&& currPoints.size() >= 2) {
			Strip s = new Strip(batchSize, new Array<Vector2>(tristrip));
			strips.add(s);
			newArrow = false;
			// Add another path
			// Line l = new Line(getArrowPolygon(currPoints));
			Line l = new Line(new ArrayList<Vector2>(currPoints));
			paths.add(l);
			ArrowData a = new ArrowData(s, l.points);
			arrows.add(a);
		}
	}

	public void drawCurrentLine(GameCamera cam) {
		if (currPoints.size() >= 2) {
			batchSize = generate(currPoints, 1);
			b = generate(currPoints, -1);
			draw(cam);
		}
	}

	float wide = .3f;

	public ArrayList<Vector2> getArrowPolygon(ArrayList<Vector2> input) {
		ArrayList<Vector2> newArrow = new ArrayList<Vector2>();

		Vector2 perp = new Vector2();
		for (int i = 0; i < input.size() - 1; i++) {
			if (i < input.size() - 1) {
				Vector2 p = input.get(i);
				Vector2 p2 = input.get(i + 1);

				perp.set(p).sub(p2).nor();
				perp.set(perp.y, -perp.x);
				perp.scl(wide);

				Vector2 perpVector = new Vector2(p.x + perp.x, p.y + perp.y);
				newArrow.add(perpVector);
			}
		}
		for (int i = input.size() - 1 - 1; i >= 0; i--) {
			if (i == input.size() - 1 - 1) {
				Vector2 a = input.get(input.size() - 1);
				newArrow.add(a); // this would be the arrowhead.
			}

			Vector2 p = input.get(i);
			Vector2 p2 = input.get(i + 1);

			perp.set(p).sub(p2).nor();
			perp.set(perp.y, -perp.x);
			perp.scl(wide);

			perp.scl(-1f);
			Vector2 perpVector = new Vector2(p.x + perp.x, p.y + perp.y);
			newArrow.add(perpVector);
		}
		return newArrow;
	}

	public void draw(GameCamera cam) {
		if (tristrip.size <= 0)
			return;
		gl20.begin(cam.combined, GL20.GL_TRIANGLE_STRIP);
		for (int i = 0; i < tristrip.size; i++) {
			if (i == batchSize) {
				gl20.end();
				gl20.begin(cam.combined, GL20.GL_TRIANGLE_STRIP);
			}
			Vector2 point = tristrip.get(i);
			gl20.color(color.r, color.g, color.b, color.a);
			gl20.vertex(point.x, point.y, 0f);
		}
		gl20.end();
	}

	int arrowIndex = 0;

	public ArrowData getNextArrow() {
		ArrowData a;
		if (arrows.size() > 0 && arrowIndex < arrows.size()) {
			a = arrows.get(arrowIndex);
			arrowIndex++;
			return a;
		}
		return null;
	}

	public static int iterations = 2;
	public static float simplifyTolerance = 35f;

	// https://github.com/mattdesl/lwjgl-basics/wiki/LibGDX-Finger-Swipe
	public static void simplify(ArrayList<Vector2> points, float sqTolerance,
			ArrayList<Vector2> out) {
		int len = points.size();

		Vector2 point = new Vector2();
		Vector2 prevPoint = points.get(0);

		out.clear();
		out.add(prevPoint);

		for (int i = 1; i < len; i++) {
			point = points.get(i);
			if (distSq(point, prevPoint) > sqTolerance) {
				out.add(point);
				prevPoint = point;
			}
		}
		if (!prevPoint.equals(point)) {
			out.add(point);
		}
	}

	public static float distSq(Vector2 p1, Vector2 p2) {
		float dx = p1.x - p2.x, dy = p1.y - p2.y;
		return dx * dx + dy * dy;
	}

	public static void smooth(ArrayList<Vector2> input,
			ArrayList<Vector2> output) {
		// expected size
		output.clear();
		output.ensureCapacity(input.size() * 2);

		// first element
		output.add(input.get(0));
		// average elements
		for (int i = 0; i < input.size() - 1; i++) {
			Vector2 p0 = input.get(i);
			Vector2 p1 = input.get(i + 1);

			Vector2 Q = new Vector2(0.75f * p0.x + 0.25f * p1.x, 0.75f * p0.y
					+ 0.25f * p1.y);
			Vector2 R = new Vector2(0.25f * p0.x + 0.75f * p1.x, 0.25f * p0.y
					+ 0.75f * p1.y);
			output.add(Q);
			output.add(R);
		}

		// last element
		output.add(input.get(input.size() - 1));
	}

	private class Line {
		public ArrayList<Vector2> points;

		public Line(ArrayList<Vector2> points) {
			this.points = points;

		}

		public int size() {
			return points.size();
		}
	}

	private int generate(ArrayList<Vector2> currPoints2, int mult) {
		int c = tristrip.size;
		// if (endcap <= 0) {
		// tristrip.add(currPoints2.get(0)); // front tip
		// } else {
		// Vector2 a = currPoints2.get(0);
		// Vector2 a2 = currPoints2.get(1);
		// perp.set(a).sub(a2).mul(endcap);
		// tristrip.add(new Vector2(a.x + perp.x, a.y + perp.y));
		// }
		texcoord.add(new Vector2(0f, 0f));

		for (int i = 0; i < currPoints2.size() - 1; i++) {
			Vector2 p = currPoints2.get(i);
			Vector2 p2 = currPoints2.get(i + 1);

			// get direction and normalize it
			perp.set(p).sub(p2).nor();

			// get perpendicular
			perp.set(-perp.y, perp.x);

			 float thick = thickness * (1f-((i)/(float)(currPoints2.size())));
			//float thick = thickness;
			// move outward by thickness
			perp.mul(thick / 2f);

			// decide on which side we are using
			perp.mul(mult);

			// add the tip of perpendicular
			tristrip.add(new Vector2(p.x + perp.x, p.y + perp.y));
			// 0.0 -> end, transparent
			texcoord.add(new Vector2(0f, 0f));

			// add the center point
			tristrip.add(new Vector2(p.x, p.y));
			// 1.0 -> center, opaque
			texcoord.add(new Vector2(1f, 0f));
		}

		// final point
		if (endcap <= 0) {
			tristrip.add(currPoints2.get(currPoints2.size() - 1)); // back tip
		} else {
			Vector2 p = currPoints2.get(currPoints2.size() - 2);
			Vector2 p2 = currPoints2.get(currPoints2.size() - 1);
			perp.set(p2).sub(p).mul(endcap);
			tristrip.add(new Vector2(p2.x + perp.x, p2.y + perp.y));
		}
		// end cap is transparent
		texcoord.add(new Vector2(0f, 0f));
		return tristrip.size - c;
	}

	public class Strip {
		public int batchSize;
		public Array<Vector2> array;

		public Strip(int b, Array<Vector2> array) {
			batchSize = b;
			this.array = array;
		}

	}

	public class ArrowData {
		public Strip strip;
		public ArrayList<Vector2> points;
		public Vector2 headPosition;

		public ArrowData(Strip strip, ArrayList<Vector2> points) {
			this.strip = strip;
			this.points = points;
			headPosition = points.get(points.size() - 1);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/*
	 * ArrayList<Vector2> outline = new ArrayList<Vector2>();
	 * void drawDebug() {
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
		*/
	 
}
