package entities;

import box2DLights.PointLight;
import box2DLights.RayHandler;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.World;

public class Light {
	RayHandler handler;
	PointLight pointLight;
	PointLight pointLight2;
	
	public Light(World world2) {
		RayHandler.useDiffuseLight(true);
		handler = new RayHandler(world2);
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
	}
	
	public void draw(GameCamera cam) {
		// Update lights.
				// pointLight.setPosition(world.bob.position.x, world2.bob.position.y);
				// pointLight2.setPosition(world.bob.position.x, world.bob.position.y);
				// handler.setCombinedMatrix(cam.combined, cam.position.x,
				// cam.position.y,
				// cam.viewportWidth * cam.zoom, cam.viewportHeight * cam.zoom);
				
				
		//handler.updateAndRender();
	}
	
}