package Helpers;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Strip {
	public int batchSize;
	public Array<Vector2> array;

	public Strip(int b ,Array<Vector2> array) {
		batchSize = b;
		this.array = array;
	}

}