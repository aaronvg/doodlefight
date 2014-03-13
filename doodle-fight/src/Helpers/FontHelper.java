package Helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;

/*
 * All the words/text that is added here gets their own bounds
 * This class helps determine whether a specific word or text button was pressed,
 * and also helps us draw this text in a special font type.
 */
public class FontHelper {
	
	BitmapFont font;
	FreeTypeFontGenerator generator;
	ArrayList<TextBundle> textArray;
	HashMap<String, TextBundle> map;
	
	public FontHelper(String filePath) {
		generator = new FreeTypeFontGenerator(Gdx.files.internal("data/fonts/Piximisa.ttf"));
		font = generator.generateFont(40); // font size 40 pixels
		font.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		font.setColor(Color.WHITE);
		map = new HashMap<String, TextBundle>();
	}
	
	public void setSize(int num) {
		font = generator.generateFont(num);
		font.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
	}
	
	public void setColor(Color c) {
		font.setColor(c);
	}
	
	public void addText(String text, float x, float y) {
		map.put(text, new TextBundle(text, x, y));
	}
	
	// Draws a specific button
	public void draw(SpriteBatch batcher, String str) {
		
		TextBundle textObject = map.get(str);
		if(textObject != null)
			font.draw(batcher, textObject.str, textObject.x, textObject.y);
	}
	
	// Draws all the TextButtons we've given to the fontHelper
	public void drawAll(SpriteBatch batcher) {
		for (TextBundle textObject : map.values()) {
		    font.draw(batcher, textObject.str, textObject.x, textObject.y);
		}
	}
	
	public float getWidth(String str) {
		return font.getBounds(str).width;

	}
	
	public float getHeight(String str) {
		return font.getBounds(str).height;
	}
	
	public boolean isPressed(String str, float x, float y) {
		TextBundle textObject = map.get(str);
		if(textObject != null) {
			// The rectangle's y coordinate starts at object.y - objectHeight because
			// text gets drawn from upper left corner, whilst rectangles start at the lower left.
			Rectangle rect = new Rectangle(textObject.x, textObject.y - getHeight(str), getWidth(str), getHeight(str));
			if(rect.contains(x, y)) {
				return true;
			}
		}
		return false;

	}
	
	public void dispose() {
		generator.dispose();
	}
	
	private class TextBundle {
		public String str;
		float x, y;
		public TextBundle(String str, float x, float y) {
			this.str = str;
			this.x = x;
			this.y = y;
		}
	}
	
	
}
