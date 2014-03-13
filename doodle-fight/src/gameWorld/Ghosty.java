package gameWorld;



import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Pool;


public class Ghosty extends DynamicGameObject {
	public static final int GHOST_STATE_JUMP = 0;
	public static final int GHOST_STATE_FALL = 1;
	public static final int GHOST_STATE_HIT = 2;
	public static final float GHOST_JUMP_VELOCITY = 11;
	public static final float GHOST_MOVE_VELOCITY = 20;
	public static final float GHOST_WIDTH = 0.8f;
	public static final float GHOST_HEIGHT = 0.8f;
	public static final float ACCELERATION = 5f;
	public static float DAMPING = .999f;
	public static float LERP = .08f;
	public static final float SPEED = 8f;
	float dampingCounter;
	int state;
	float stateTime;
	float dirX;
	float dirY;
	protected Rectangle bounds;
	Vector2 directionVector;
	boolean first;
	
	
	float angleBaby;

	Vector2 position2;

	Body circleBody;
	World world;

	private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
		@Override
		protected Rectangle newObject() {
			return new Rectangle();
		}
	};

	public Ghosty(float x, float y, World world2) {
		super(x, y, GHOST_WIDTH, GHOST_HEIGHT);
		state = GHOST_STATE_FALL;
		stateTime = 0;
		dirX = x;
		dirY = y;
		// width is 1/32f * texture.width
		bounds = new Rectangle(position.x, position.y, GHOST_WIDTH,
				GHOST_HEIGHT);
		position2 = new Vector2(x, y);

		directionVector = new Vector2();

		dampingCounter = 0;
		// Box2dstuff: create a circular rigidBody for our ghost
		// Create definition of a collision body (a square)
		world = world2;
		BodyDef circleDef = new BodyDef();
		circleDef.type = BodyType.DynamicBody;
		circleDef.position.set(x + .5f, y + .5f);

		// Create shape for that definition
		circleBody = world.createBody(circleDef);
		CircleShape circleShape = new CircleShape();
		circleShape.setRadius(.55f);
		circleBody.setFixedRotation(true);

		// Unite them in one fixture object.
		FixtureDef circleFixture = new FixtureDef();
		circleFixture.shape = circleShape;
		circleFixture.density = .4f;
		circleFixture.friction = .2f;
		circleFixture.restitution = .6f;
		circleBody.createFixture(circleFixture);
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public void clampVelocity() {
		velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION, ACCELERATION);
		velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION, ACCELERATION);
	}

	public void update(float deltaTime) {
		//clampVelocity();
		
		position2.x += (position.x - position2.x) * LERP;
		position2.y += (position.y - position2.y) * LERP;


		// ------------------------box2dstuff
		// circleBody.
		// physics update
		

		double deg = Math.toRadians(0);
		if (Gdx.input.isTouched()) {
			dampingCounter += .06f;
			if (dampingCounter > DAMPING) {
				dampingCounter = DAMPING;
			}
			// dampingCounter = damping;
			velocity.x = (float) ((-Math.sin(deg)) * SPEED * dampingCounter);
			velocity.y = (float) ((Math.cos(deg)) * SPEED * dampingCounter);
			circleBody.setLinearVelocity(velocity);
			velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION,
					ACCELERATION);
			velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION,
					ACCELERATION);
		} else {
			velocity.x = (float) ((-Math.sin(deg)) * SPEED);
			velocity.y = (float) ((Math.cos(deg)) * SPEED);
			dampingCounter *= DAMPING;
			velocity.scl(dampingCounter);
			//Log.d("damping", "hi" + Float.toString(dampingCounter));
			circleBody.setLinearVelocity(velocity);
			velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION,
					ACCELERATION);
			velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION,
					ACCELERATION);

		}
		
		directionVector.x = (float) ((-Math.sin(deg)) * SPEED);
		directionVector.y = (float) ((Math.cos(deg)) * SPEED);

		position.x = circleBody.getPosition().x;
		position.y = circleBody.getPosition().y;
		/*
		if (velocity.y > 0 && state != GHOST_STATE_HIT) {
			if (state != GHOST_STATE_JUMP) {
				state = GHOST_STATE_JUMP;
				stateTime = 0;
			}
		}

		if (velocity.y < 0 && state != GHOST_STATE_HIT) {
			if (state != GHOST_STATE_FALL) {
				state = GHOST_STATE_FALL;
				stateTime = 0;
			}
		}*/

		// if (position.x < 0) position.x = World.WORLD_WIDTH;
		// if (position.x > World.WORLD_WIDTH) position.x = 0;

		stateTime += deltaTime;
	}
	
	// for when the world itself stays the same (we are rotating it but it looks the same)
	public void update2(float deltaTime) {

		position2.x += (position.x - position2.x) * LERP;
		position2.y += (position.y - position2.y) * LERP;

		angleBaby = LerpDegrees(angleBaby, 0, LERP);

		// ------------------------box2dstuff
		// circleBody.
		// physics update
		
	
		double deg = Math.toRadians(0);
		if (Gdx.input.isTouched()) {
			dampingCounter += .06f;
			if (dampingCounter > DAMPING) {
				dampingCounter = DAMPING;
			}
			// dampingCounter = damping;
			velocity.x = (float) ((Math.cos(deg)) * SPEED * dampingCounter); // the angles are the only change for rotationmodes
			velocity.y = (float) ((-Math.sin(deg)) * SPEED * dampingCounter);
			circleBody.setLinearVelocity(velocity);
			velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION,
					ACCELERATION);
			velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION,
					ACCELERATION);
			
		} else {
			velocity.x = (float) ((Math.cos(deg)) * SPEED);
			velocity.y = (float) ((-Math.sin(deg)) * SPEED);
			dampingCounter *= DAMPING;
			velocity.scl(dampingCounter);
			//Log.d("damping", "hi" + Float.toString(dampingCounter));
			circleBody.setLinearVelocity(velocity);
			velocity.x = MathUtils.clamp(velocity.x, -ACCELERATION,
					ACCELERATION);
			velocity.y = MathUtils.clamp(velocity.y, -ACCELERATION,
					ACCELERATION);

		}
		
		directionVector.x = (float) ((Math.cos(deg)) * SPEED);
		directionVector.y = (float) ((-Math.sin(deg)) * SPEED);

		position.x = circleBody.getPosition().x;
		position.y = circleBody.getPosition().y;
		/*
		if (velocity.y > 0 && state != GHOST_STATE_HIT) {
			if (state != GHOST_STATE_JUMP) {
				state = GHOST_STATE_JUMP;
				stateTime = 0;
			}
		}

		if (velocity.y < 0 && state != GHOST_STATE_HIT) {
			if (state != GHOST_STATE_FALL) {
				state = GHOST_STATE_FALL;
				stateTime = 0;
			}
		}*/

		// if (position.x < 0) position.x = World.WORLD_WIDTH;
		// if (position.x > World.WORLD_WIDTH) position.x = 0;

		stateTime += deltaTime;
	}
	
	
	
	
	
	
	
	

	public static float LerpDegrees(float start, float end, float amount) {
		float difference = Math.abs(end - start);
		if (difference > 180) {
			// We need to add on to one of the values.
			if (end > start) {
				// We'll add it on to start...
				start += 360;
			} else {
				// Add it on to end.
				end += 360;
			}
		}

		// Interpolate it.
		float value = (start + ((end - start) * amount));

		// Wrap it..
		float rangeZero = 360;

		if (value >= 0 && value <= 360)
			return value;

		return (value % rangeZero);
	}

	public void hitSquirrel() {
		velocity.set(0, 0);
		state = GHOST_STATE_HIT;
		stateTime = 0;
	}

	public void hitPlatform() {
		velocity.y = GHOST_JUMP_VELOCITY;
		state = GHOST_STATE_JUMP;
		stateTime = 0;
	}

	public void hitSpring() {
		velocity.y = GHOST_JUMP_VELOCITY * 1.5f;
		state = GHOST_STATE_JUMP;
		stateTime = 0;
	}
}
