package entities;

import java.util.ArrayList;

import mdesl.swipe.SwipeHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * This class controls all input to the game. It also converts drawings to box2D
 * objects and manages drawn paths.
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
	int batchSize;
	Vector2 perp = new Vector2();
	public float thickness = .2f;
	public float endcap = 0f;
	public Color color = new Color(Color.BLACK);

	SwipeHandler swipe;

	public DrawingManager(SwipeHandler s) {
		gl20 = new ImmediateModeRenderer20(false, true, 1);
		newPath = true;
		currPoints = new ArrayList<Vector2>();
		paths = new ArrayList<Line>();
		shaper = new ShapeRenderer();
		shaper.setColor(Color.BLACK);

		smoothLine = new ArrayList<Vector2>();
		smoothLine2 = new ArrayList<Vector2>();

		swipe = s;
	}

	int b;
	boolean newArrow = false;
	public void update(GameCamera cam, SpriteBatch batch) {
		/*if (Gdx.input.isTouched()) {
			// Start drawing path.
			Vector3 point = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
			cam.unproject(point);
			currPoints.add(new Vector2(point.x, point.y));
		} else {
			// if we were drawing a path, save it now into our buffer.
			if(currPoints.size() > 0) {
				Line l = new Line(new ArrayList<Vector2>(currPoints));
				paths.add(l);
				currPoints.clear();
			}
		}
		*/


		if (Gdx.input.isTouched()) {
			newArrow = true;
			// Start drawing path. save it.
			currPoints = swipe.input2();
			for (int i = 0; i < currPoints.size(); i++) {
				Vector3 point = new Vector3(currPoints.get(i).x,
						currPoints.get(i).y, 1);
				cam.unproject(point);
				currPoints.set(i, new Vector2(point.x, point.y));
			}
		} else {
			// if we were drawing a path, save it now into our buffer.
			if (currPoints.size() > 0) {
				Line l = new Line(new ArrayList<Vector2>(currPoints));
				paths.add(l);
				//currPoints.clear();
			}
		}

		tristrip.clear();
		texcoord.clear();
		if (currPoints.size() < 2)
			return;
		// Smooth out current line
		// smooth(currPoints, smoothLine);

		batchSize = generate(currPoints, 1);
		b = generate(currPoints, -1);
		draw(cam);
		
		// Activated only once per drawing. (not touched, and theres an arrow waiting in the buffer)
		if(!Gdx.input.isTouched() && newArrow == true) {
			Strip s = new Strip(batchSize, new Array<Vector2>(tristrip));
			strips.add(s);
			Gdx.app.log("arrow", "Total " + strips.size());
			newArrow = false;
		}
		
		draw2(cam);
		
		// draw the rest of the paths.
	/*	for(int i =0; i < paths.size(); i++)
		{
			currPoints = paths.get(i).points;
			tristrip.clear();
			texcoord.clear();
			if (currPoints.size() < 2)
				return;
			// Smooth out current line
			// smooth(currPoints, smoothLine);
			batchSize = generate(currPoints, 1);
			
			draw(cam);
		}*/

		/*shaper.setProjectionMatrix(cam.combined);
		
		// Draw the current line (realtime drawing)
		shaper.begin(ShapeType.Line);
		for(int i = 0; i < smoothLine.size() - 1; i++) {
			
			shaper.line(smoothLine.get(i), smoothLine.get(i + 1));
			
		}
		shaper.end();
		
		
		// Draw all previous lines.
		for (int i = 0; i < paths.size(); i++) {
			shaper.begin(ShapeType.Line);
			ArrayList<Vector2> points = paths.get(i).points;
			for(int j = 0; j < points.size() - 1; j++) {
				shaper.line(points.get(j), points.get(j+1));
				
			}
			
			shaper.end();
		}
		*/
	}

	// TODO: simply make an array of tristrips, which we already calculate every frame.
	// on touch up: add the tristrip array to our array of tristrips.
	// now in this draw method, iterate through ALL the arrays, not just the first one, and
	//repeat the algorithm.
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
			//Vector2 tc = texcoord.get(i);
			gl20.color(color.r, color.g, color.b, color.a);
			//gl20.texCoord(tc.x, 0f);
			gl20.vertex(point.x, point.y, 0f);
		}
		gl20.end();
	}
	
	
	public void draw2(GameCamera cam) {
		int size;
		Gdx.app.log("draw", "strips size " + strips.size());
		for(int j = 0; j < strips.size(); j++) {
			Array<Vector2> strip = strips.get(j).array;
			size = strips.get(j).batchSize;
			
			if (strip.size <= 0)
				return;
			gl20.begin(cam.combined, GL20.GL_TRIANGLE_STRIP);
			for (int i = 0; i < strip.size; i++) {
				if (i == size) {
					gl20.end();
					gl20.begin(cam.combined, GL20.GL_TRIANGLE_STRIP);
				}
				Vector2 point = strip.get(i);
				//Vector2 tc = texcoord.get(i);
				gl20.color(color.r, color.g, color.b, color.a);
				//gl20.texCoord(tc.x, 0f);
				gl20.vertex(point.x, point.y, 0f);
			}
			gl20.end();
			
			
			
			
		}
		
		
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

	
	// TODO: make the arrow not follow the finger. meaning the points that are there
	// should stay there. Just stop adding more after certain number of points (don't do the insertion thing)
	private int generate(ArrayList<Vector2> currPoints2, int mult) {
		int c = tristrip.size;
		if (endcap <= 0) {
		//	tristrip.add(currPoints2.get(0)); // front tip
		} else {
			Vector2 p = currPoints2.get(0);
			Vector2 p2 = currPoints2.get(1);
			perp.set(p).sub(p2).mul(endcap);
			tristrip.add(new Vector2(p.x + perp.x, p.y + perp.y));
		}
		texcoord.add(new Vector2(0f, 0f));

		for (int i = 1; i < currPoints2.size() - 1; i++) {
			Vector2 p = currPoints2.get(i);
			Vector2 p2 = currPoints2.get(i + 1);

			// get direction and normalize it
			perp.set(p).sub(p2).nor();

			// get perpendicular
			perp.set(-perp.y, perp.x);

			// float thick = thickness * (1f-((i)/(float)(currPoints2.size())));
			float thick = thickness;
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
			tristrip.add(currPoints2.get(currPoints2.size() - 1));  //back tip
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
	
	
	private class Strip {
		int batchSize;
		Array<Vector2> array;
		public Strip(int b, Array<Vector2> array) {
			batchSize = b;
			this.array = array;
		}
		
		
		
	}

}