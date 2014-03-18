package gameWorld;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.Shape;

public class CustomContactListener implements ContactListener {
	
	public CustomContactListener() {
		
	}

	@Override
	public void beginContact(Contact contact) {
		// TODO Auto-generated method stub
		
	
		
		
	}

	@Override
	public void endContact(Contact contact) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
		// TODO Auto-generated method stub
		Body a = contact.getFixtureA().getBody();
		Body b = contact.getFixtureB().getBody();
		
		if(a.getUserData() instanceof Shape && b.getUserData() instanceof Arrow) {
			
			Arrow arrow = (Arrow)b.getUserData();
			arrow.freeFlight = false;
			Gdx.app.log("collision", "arrow hit a wall!");
		}
		
		if(b.getUserData() instanceof Shape && a.getUserData() instanceof Arrow) {
			Arrow arrow = (Arrow)a.getUserData();
			arrow.freeFlight = false;
			Gdx.app.log("collision", "arrow hit a wall!");
		}
		
		
	}
	
	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {

		Body a = contact.getFixtureA().getBody();
		Body b = contact.getFixtureB().getBody();
		
		float[] impulses = impulse.getNormalImpulses();
		Gdx.app.log("collision", "normals length " + impulses.length + " " + impulses[0] + " " + impulses[1]);
		if(a.getUserData() instanceof Shape && b.getUserData() instanceof Arrow && impulses[0] > 2) {
			
			Arrow arrow = (Arrow)b.getUserData();
		//	Gdx.app.log("collision", "arrow hit a wall!");
			arrow.stick = true;
			arrow.target = a;
		}
		
		if(b.getUserData() instanceof Shape && a.getUserData() instanceof Arrow && impulses[0] > 2) {
			Arrow arrow = (Arrow)a.getUserData();
		//	Gdx.app.log("collision", "arrow hit a wall!");
			arrow.stick = true;
			arrow.target = b; // sets target to this body
		}
	}

}
