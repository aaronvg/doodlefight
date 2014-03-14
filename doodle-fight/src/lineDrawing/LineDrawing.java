package lineDrawing;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GLCommon;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;


public class LineDrawing implements ApplicationListener {
	
	
	public SpriteBatch spriteBatch;
	public ShapeRenderer shapeRenderer;
	public OrthographicCamera camera;
	public int screenWidth;
	public int screenHeight;
	
	private boolean _collect = false;
	private List<Vector2> _points;
	private int _maxDistance = 20;
	private LinePath2D _linePath;
	
	//space between dashes or dots
	private float _gap = 20f;
	
	private Plane _plane;
	private float _planeAnimationTime = 0f;
	//the plane speed
	private float _speed = 0.5f;
	private float _speedOnPath = 0f;
			
	private float _progress = 0f;
	private float _oldLength = 0f;
	private float _oldProgress = 0f;
	
	//rotation easing
	private float _dr = 0f;
	private float _ar = 0f;
	private float _vr = 0f;
	private float _targetRotation = 0f;
	private float _rotationSpring = 0.1f;
	private float _rotationDamping = 0.6f;
	
	private Vector3 _touchPoint;
	private PointPool _vectorPool;
	
	@Override
	public void create() {
		
		screenWidth = 480;
		screenHeight = 320;
		
		camera = new OrthographicCamera(screenWidth, screenHeight);
		camera.position.set(screenWidth * 0.5f, screenHeight * 0.5f, 0);
		
		
		spriteBatch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		
		Assets.load();
		
		_linePath = new LinePath2D();
		_plane = new Plane(50, 50);
		_plane.setSkin(Assets.planeFrame1);
		
		
		_touchPoint = new Vector3();
		_points = new ArrayList<Vector2>();
		
		_vectorPool = new PointPool();
				
		//set the point size for the path
		Gdx.gl11.glPointSize(3.0f);

	}

	private void updateSpeedAndProgress () {
		float iterations = (_linePath.totalLength/_speed);
		_speedOnPath = 1/iterations;
		
		if (_oldLength != 0 && _linePath.totalLength - _oldLength > 10) {
			_progress = (_oldLength * _oldProgress)/_linePath.totalLength;
			_linePath.renderObjectAt(_plane, _progress);
		}
		_oldLength = _linePath.totalLength;
		_oldProgress = _progress;
	}
	
	
	@Override
	public void render() {
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		//grab user input
		if (Gdx.input.isTouched(0)) {
			
			camera.unproject(_touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0));
			
			if (_collect) {
			
				Vector2 point = _vectorPool.getObject();
				point.set(_touchPoint.x, _touchPoint.y);
				
				if (_points.size() == 0) {
					_linePath.points.clear();
					_progress = 0f;
					point.set(_plane.x, _plane.y);
					_points.add(point);
					_linePath.insertMultiplePoints(_points, 0);
				} else {
					
					Vector2 lastPoint = _points.get(_points.size() - 1);
					//only add a point if the distance to the previous point is long enough 
					//(you don't want to add the same point over and over again!) 
					if (point.dst(lastPoint) > _maxDistance) {
						_points.add(point);
						_linePath.appendPoint(point);
						updateSpeedAndProgress();
						
					}
				}
			} else {
				_points.clear();
				_linePath.points.clear();
				_collect = false;
				if (_plane.bounds().contains(_touchPoint.x, _touchPoint.y)) {
					_collect = true;
				} 
			}
		} else{
			_collect = false;
		}
		
		if (Gdx.input.justTouched()) {
			_collect = false;
		}
		
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		//update plane
		//is path has not been set, or plane has reached the end of the path, keep moving plane
		if (_linePath.points.size() <= 1 || _progress + _speedOnPath >= 1 || _linePath.totalLength < 5) {

			float angle = _plane.rotation * (float) Math.PI /  180;
			
			_plane.x = (float) (_plane.x + _speed * Math.cos(angle));
			_plane.y = (float) (_plane.y + _speed * Math.sin(angle)); 
			
		} else {
			//increment progress with our calculated speedOnPath
			//this ensures the plane keeps moving at the speed set in _speed variable
			_progress += _speedOnPath;
			if (_linePath.points.size() > 1) _linePath.renderObjectAt(_plane, _progress);
			//set target rotation for ease/spring logic bellow
			_targetRotation = _linePath.angle;
			if (_targetRotation > _plane.rotation + 180) _targetRotation -= 360;
			if (_targetRotation < _plane.rotation - 180) _targetRotation += 360;
			
			//redraw the line so dashes/dots already transposed by plane are not redrawn. meaning, they are erased.
			_oldProgress = _progress;
			
			//ease and spring the rotation a bit so it looks nicer!
			_dr = _targetRotation - _plane.rotation;
			_ar = _dr * _rotationSpring;
			_vr += _ar;
			_vr *= _rotationDamping;
			_plane.rotation +=  _vr;
		}
		
		
		
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		//render
		GLCommon gl = Gdx.gl;
		gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		camera.update();
		 
			
		//draw line
		if (_linePath.totalLength > 100 &&  _linePath.points.size() != 0 && _points.size() != 0)  {
			
			float points  = _linePath.totalLength / _gap;
			float progressIncrement = 1/points;
			
			//spread the points evenly
			float p = 0f;
			Vector2 point;
			
			shapeRenderer.setProjectionMatrix(camera.combined);
			shapeRenderer.begin(ShapeType.Point);
			
			 
			while (p < 1 ) {
				//in order to draw the path, traverse it with a fixed progress and grab the points from that
				point = _linePath.getPointAtProgress(p);
				
				//check p against progress so we don't draw the dashes/dots the plane has moved past already
				if (point != null && p > _linePath.progress + 0.01) {
					shapeRenderer.point(point.x, point.y, 0);
				}
				p += progressIncrement;
			}
			shapeRenderer.end();
		}
		
		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.enableBlending();
		spriteBatch.begin();
		//draw plane
		_plane.skin = Assets.planeAnimation.getKeyFrame(_planeAnimationTime, true);
		//**draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) 
		spriteBatch.draw(_plane.skin, _plane.x - _plane.width * 0.5f, _plane.y - _plane.height * 0.5f, _plane.width*0.5f, _plane.height*0.5f, _plane.width, _plane.height, 1, 1, _plane.rotation);
		_planeAnimationTime += Gdx.graphics.getDeltaTime();
		if (_planeAnimationTime > 10) _planeAnimationTime = 0;
		
		spriteBatch.end();
	}
		

	@Override
	public void resume() {}
	@Override
	public void resize(int arg0, int arg1) {}
	@Override
	public void dispose() {
		_linePath.dispose();
		_vectorPool.dispose();
	}
	@Override
	public void pause() {}
}
