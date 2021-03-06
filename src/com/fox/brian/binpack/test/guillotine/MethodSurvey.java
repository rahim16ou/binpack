package com.fox.brian.binpack.test.guillotine;

import org.junit.Test;

import com.fox.brian.binpack.algorithms.GuillotineContainer;

public class MethodSurvey extends AbstractTest {


	@Test
	public void incrementalTest() {

		for ( GuillotineContainer.FreeRectChoiceHeuristic h :  GuillotineContainer.FreeRectChoiceHeuristic.values()) {
			for ( GuillotineContainer.GuillotineSplitHeuristic s :  GuillotineContainer.GuillotineSplitHeuristic.values()) {
				float[] vals = new float[100];
				for (int i = 0; i < 100; i++) 
					vals[i] = i;
				
				// Usage Example: MaxRectsBinPackTest 256 256 30 20 50 20 10 80 90 20
				float cap = pack(
							256.0F, 
							256.0F, 
							vals, 
							h, 
							s,
							false
							);	
					
				System.out.printf("Heuristic: %-20s   Remaining Capacity: %.2f%%\n", h.name(),cap);

			}
		}
	}


}
