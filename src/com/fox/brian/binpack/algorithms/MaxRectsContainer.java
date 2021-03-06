package com.fox.brian.binpack.algorithms;

import java.util.ArrayList;
import java.util.List;

import com.fox.brian.binpack.porthacks.outFloat;
import com.fox.brian.binpack.util.Helper;
import com.fox.brian.binpack.util.Rect;

public class MaxRectsContainer<T> extends Container<T> {
	
	private	float binWidth;
	private	float binHeight;

	private List<Rect> usedRectangles;
	private List<Rect> freeRectangles;



	/**
	 * Instantiates a bin of size (0,0). Call Init to create a new bin.
	 */
	public MaxRectsContainer() {
		this(0,0);
	}

	/**
	 * Instantiates a bin of the given size.
	 * @param binWidth2
	 * @param binHeight2
	 */
	public MaxRectsContainer(float binWidth2, float binHeight2) {
		usedRectangles = new ArrayList<Rect>();;
		freeRectangles = new ArrayList<Rect>();
		init(binWidth2, binHeight2);
	}

	/** 
	 * (Re)initializes the packer to an empty bin of width x height units. Call whenever you need to restart with a new bin.
	 * @param binWidth2
	 * @param binHeight2
	 */
	public void init(float binWidth2, float binHeight2) {
		binWidth = binWidth2;
		binHeight = binHeight2;

		Rect n = new Rect(0,0,binWidth2, binHeight2);

		usedRectangles.clear();
		freeRectangles.clear();
		freeRectangles.add(n);		
	}

	
	/** 
	 * Specifies the different heuristic rules that can be used when 
	 * deciding where to place a new rectangle.
	 *
	 */
	public enum FreeRectChoiceHeuristic
	{
		/* Positions the rectangle against the short side of a free rectangle 
		 * into which it fits the best. */
		RectBestShortSideFit, 
		
		/* BLSF: Positions the rectangle against the long side of a free 
		 * rectangle into which it fits the best. */
		RectBestLongSideFit, 
		
		/* BAF: Positions the rectangle into the smallest free rect into which 
		 * it fits. */
		RectBestAreaFit,
		
		/* BL: Does the Tetris placement. */
		RectBottomLeftRule,  
		
		/* CP: Choosest the placement where the rectangle touches other rects as 
		 * much as possible. */
		RectContactPointRule 
	};

	
	/**
	 * Inserts the given list of rectangles in an offline/batch mode, 
	 * possibly rotated.
     *
	 * @param rects The list of rectangles to insert. This vector will be destroyed in the process.
	 * @param dst [out] This list will contain the packed rectangles. The indices will not correspond to that of rects.
	 * @param method The rectangle placement rule to use when packing.
	 *
	 */
	void Insert(List<Rect> rects, List<Rect> dst, FreeRectChoiceHeuristic method) {
		dst.clear();

		while(rects.size() > 0)
		{
			float bestScore1 = Float.MAX_VALUE;
			float bestScore2 = Float.MAX_VALUE;
			float bestRectIndex = -1;
			Rect bestNode = new Rect();

			for(int i = 0; i < rects.size(); ++i)
			{
				outFloat score1 = new outFloat();
				outFloat score2 = new outFloat();
				Rect newNode = ScoreRect(rects.get(i).width(), rects.get(i).height(), method, score1, score2);

				if (score1.val < bestScore1 || (score1.val == bestScore1 && score2.val < bestScore2))
				{
					bestScore1 = score1.val;
					bestScore2 = score2.val;
					bestNode = newNode;
					bestRectIndex = i;
				}
			}

			if (bestRectIndex == -1)
				return;

			PlaceRect(bestNode);
			rects.remove(bestRectIndex);
		}	
		
	}

	
	
	/** 
	 * Inserts a single rectangle into the bin, possibly rotated.
	 * @param width
	 * @param height
	 * @param method
	 * @return
	 */
	public Rect Insert(float width, float height, FreeRectChoiceHeuristic method) {
		/*
		System.out.printf("\nfreeRectangles BEFORE:\n");
		for (Rect r : freeRectangles)
			System.out.printf("    Free Rect:  X: %d    Y: %d     H: %d     W: %d\n", r.x, r.y, r.height, r.width);
		*/
		Rect newNode = new Rect();
		outFloat score1 = new outFloat(); // Unused in this function. We don't need to know the score after finding the position.
		outFloat score2 = new outFloat();
		switch(method)
		{
			case RectBestShortSideFit: 
				newNode = FindPositionForNewNodeBestShortSideFit(width, height, score1, score2); 
				break;
			case RectBottomLeftRule: 
				newNode = FindPositionForNewNodeBottomLeft(width, height, score1, score2); 
				break;
			case RectContactPointRule: 
				newNode = FindPositionForNewNodeContactPoint(width, height, score1); 
				break;
			case RectBestLongSideFit: 
				newNode = FindPositionForNewNodeBestLongSideFit(width, height, score2, score1); 
				break;
			case RectBestAreaFit: 
				newNode = FindPositionForNewNodeBestAreaFit(width, height, score1, score2); 
				break;
		}
			
		if (newNode.height() == 0)
			return newNode;

		
		int numRectanglesToProcess = freeRectangles.size();
		for(int i = 0; i < numRectanglesToProcess; ++i)
		{
			if (SplitFreeNode(freeRectangles.get(i), newNode))
			{
				freeRectangles.remove(i);
				--i;
				--numRectanglesToProcess;
			}
		}
		
		/*
		System.out.printf("freeRectangles BEFORE PRUNE:\n");
		for (Rect r : freeRectangles)
			System.out.printf("    Free Rect:  X: %d    Y: %d     H: %d     W: %d\n", r.x, r.y, r.height, r.width);
		*/
		
		PruneFreeList();
		usedRectangles.add(newNode);

		/*
		System.out.printf("Score:  W: %d    H: %d    S1: %d    S2: %d\n", width, height, score1.val, score2.val);
		System.out.printf("freeRectangles AFTER:\n");
		for (Rect r : freeRectangles)
			System.out.printf("    Free Rect:  X: %d    Y: %d     H: %d     W: %d\n", r.x, r.y, r.height, r.width);
		*/
		
		return newNode;

	}

	/**
	 * Computes the ratio of used surface area to the total bin area.
	 * @return
	 */
	public float Occupancy() {
		long usedSurfaceArea = 0;
		for(int i = 0; i < usedRectangles.size(); ++i)
			usedSurfaceArea += usedRectangles.get(i).width() * usedRectangles.get(i).height();
		return usedSurfaceArea / (binWidth * binHeight);
	};

	
	/** Computes the placement score for placing the given rectangle with the given method.
	 * 
	 * @param width
	 * @param height
	 * @param method
	 * @param score1 [out] The primary placement score will be outputted here.
	 * @param score2 [out] The secondary placement score will be outputted here. This isu sed to break ties.
	 * @return Rect that identifies where the rectangle would be placed if it were placed.
	 */
	private Rect ScoreRect(float width, float height, FreeRectChoiceHeuristic method, outFloat score1, outFloat score2) {
		Rect newNode = new Rect();
		score1.val = Float.MAX_VALUE;
		score2.val = Float.MAX_VALUE;
		switch(method) {
			case RectBestShortSideFit:
				newNode = FindPositionForNewNodeBestShortSideFit(width, height,score1, score2);
				break;
			case RectBottomLeftRule:
				newNode = FindPositionForNewNodeBottomLeft(width, height, score1,score2);
				break;
			case RectContactPointRule:
				newNode = FindPositionForNewNodeContactPoint(width, height, score1);
				/* Reverse since we are minimizing, but for contact point score bigger 
				   is better. */
				score1.val = -score1.val; 
				break;
			case RectBestLongSideFit:
				newNode = FindPositionForNewNodeBestLongSideFit(width, height, score2, score1);
				break;
			case RectBestAreaFit:
				newNode = FindPositionForNewNodeBestAreaFit(width, height, score1, score2);
				break;
		}

		// Cannot fit the current rectangle.
		if (newNode.height() == 0)
		{
			score1.val = Float.MAX_VALUE;
			score2.val = Float.MAX_VALUE;
		}

		return newNode;	
	}

	/**
	 * Places the given rectangle into the bin.
	 */
	private void PlaceRect(Rect node) {

		int numRectanglesToProcess = freeRectangles.size();
		for(int i = 0; i < numRectanglesToProcess; ++i)
		{
			if (SplitFreeNode(freeRectangles.get(i), node))
			{
				freeRectangles.remove(i);
				--i;
				--numRectanglesToProcess;
			}
		}

		PruneFreeList();

		usedRectangles.add(node);
		//		dst.push_back(bestNode); ///\todo Refactor so that this compiles.
	};

	
	/**
	 * Returns 0 if the two intervals i1 and i2 are disjoint, or the length 
	 * of their overlap otherwise.
	 * 
	 * @param i1start
	 * @param i1end
	 * @param i2start
	 * @param i2end
	 * @return
	 */
	float CommonIntervalLength(float i1start, float i1end, float i2start, float i2end)
	{
		if (i1end < i2start || i2end < i1start)
			return 0;
		return Helper.min(i1end, i2end) - Helper.max(i1start, i2start);
	}
	
	
	/**
	 * Computes the placement score for the -CP variant.
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
	private int ContactPointScoreNode(float x, float y, float width, float height) { 
		int score = 0;

		if (x == 0 || x + width == binWidth)
			score += height;
		if (y == 0 || y + height == binHeight)
			score += width;

		for(int i = 0; i < usedRectangles.size(); ++i)
		{
			if (usedRectangles.get(i).x() == x + width 
					|| usedRectangles.get(i).x() + usedRectangles.get(i).width() == x)
				score += CommonIntervalLength(
						usedRectangles.get(i).y(), 
						usedRectangles.get(i).y() + usedRectangles.get(i).height(), 
						y, 
						y + height);
			if (usedRectangles.get(i).y() == y + height 
					|| usedRectangles.get(i).y() + usedRectangles.get(i).height() == y)
				score += CommonIntervalLength(
						usedRectangles.get(i).x(), 
						usedRectangles.get(i).x() + usedRectangles.get(i).width(), 
						x, 
						x + width);
		}
		return score;
	};

	
	
	private Rect FindPositionForNewNodeBottomLeft(float width, float height, outFloat bestY, outFloat bestX) {
		Rect bestNode = new Rect();
		// memset(&bestNode, 0, sizeof(Rect));

		bestY.val = Float.MAX_VALUE;

		for(int i = 0; i < freeRectangles.size(); ++i)
		{
			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width() >= width && freeRectangles.get(i).height() >= height)
			{
				float topSideY = freeRectangles.get(i).y() + height;
				if (topSideY < bestY.val || (topSideY == bestY.val && freeRectangles.get(i).x() < bestX.val))
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							width,
							height
					);
					bestY.val = topSideY;
					bestX.val = freeRectangles.get(i).x();
				}
			}
			if (freeRectangles.get(i).width() >= height && freeRectangles.get(i).height() >= width)
			{
				float topSideY = freeRectangles.get(i).y() + width;
				if (topSideY < bestY.val || (topSideY == bestY.val && freeRectangles.get(i).x() < bestX.val))
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							height,
							width
					);
					bestY.val = topSideY;
					bestX.val = freeRectangles.get(i).x();
				}
			}
		}
		return bestNode;
	};

	
	
	private Rect FindPositionForNewNodeBestShortSideFit(float width, float height, outFloat bestShortSideFit, outFloat bestLongSideFit) { 
		Rect bestNode = new Rect();
		// memset(&bestNode, 0, sizeof(Rect));

		bestShortSideFit.val = Float.MAX_VALUE;

		for(int i = 0; i < freeRectangles.size(); ++i)
		{
			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width() >= width && freeRectangles.get(i).height() >= height)
			{
				float leftoverHoriz = Helper.abs(freeRectangles.get(i).width() - width);
				float leftoverVert = Helper.abs(freeRectangles.get(i).height() - height);
				float shortSideFit = Helper.min(leftoverHoriz, leftoverVert);
				float longSideFit = Helper.max(leftoverHoriz, leftoverVert);

				if (shortSideFit < bestShortSideFit.val || (shortSideFit == bestShortSideFit.val && longSideFit < bestLongSideFit.val))
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							width,
							height
					);
					bestShortSideFit.val = shortSideFit;
					bestLongSideFit.val = longSideFit;
				}
			}

			if (freeRectangles.get(i).width() >= height && freeRectangles.get(i).height() >= width)
			{
				float flippedLeftoverHoriz = Helper.abs(freeRectangles.get(i).width() - height);
				float flippedLeftoverVert = Helper.abs(freeRectangles.get(i).height() - width);
				float flippedShortSideFit = Helper.min(flippedLeftoverHoriz, flippedLeftoverVert);
				float flippedLongSideFit = Helper.max(flippedLeftoverHoriz, flippedLeftoverVert);

				if (flippedShortSideFit < bestShortSideFit.val || (flippedShortSideFit == bestShortSideFit.val && flippedLongSideFit < bestLongSideFit.val))
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							height,
							width
					);
					bestShortSideFit.val = flippedShortSideFit;
					bestLongSideFit.val = flippedLongSideFit;
				}
			}
		}
		return bestNode;

	};

	
	/**
	 * 
	 * @param width
	 * @param height
	 * @param bestShortSideFit
	 * @param bestLongSideFit
	 * @return
	 */
	private Rect FindPositionForNewNodeBestLongSideFit(float width, float height, outFloat bestShortSideFit, outFloat bestLongSideFit) { 
		Rect bestNode = new Rect();
		// memset(&bestNode, 0, sizeof(Rect));

		bestLongSideFit.val = Float.MAX_VALUE;

		for(int i = 0; i < freeRectangles.size(); ++i)
		{
			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width() >= width && freeRectangles.get(i).height() >= height)
			{
				float leftoverHoriz = Helper.abs(freeRectangles.get(i).width() - width);
				float leftoverVert = Helper.abs(freeRectangles.get(i).height() - height);
				float shortSideFit = Helper.min(leftoverHoriz, leftoverVert);
				float longSideFit = Helper.max(leftoverHoriz, leftoverVert);

				if (longSideFit < bestLongSideFit.val || (longSideFit == bestLongSideFit.val && shortSideFit < bestShortSideFit.val))
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							width,
							height
					);
					bestShortSideFit.val = shortSideFit;
					bestLongSideFit.val = longSideFit;
				}
			}

			if (freeRectangles.get(i).width() >= height && freeRectangles.get(i).height() >= width)
			{
				float leftoverHoriz = Helper.abs(freeRectangles.get(i).width() - height);
				float leftoverVert = Helper.abs(freeRectangles.get(i).height() - width);
				float shortSideFit = Helper.min(leftoverHoriz, leftoverVert);
				float longSideFit = Helper.max(leftoverHoriz, leftoverVert);

				if (longSideFit < bestLongSideFit.val || (longSideFit == bestLongSideFit.val && shortSideFit < bestShortSideFit.val))
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							height,
							width
					);
					bestShortSideFit.val = shortSideFit;
					bestLongSideFit.val = longSideFit;
				}
			}
		}
		return bestNode;
	};
	
	
	/**
	 * 
	 * @param width
	 * @param height
	 * @param bestAreaFit
	 * @param bestShortSideFit
	 * @return
	 */
	private Rect FindPositionForNewNodeBestAreaFit(float width, float height, outFloat bestAreaFit, outFloat bestShortSideFit) { 
		Rect bestNode = new Rect();
		// memset(&bestNode, 0, sizeof(Rect));

		bestAreaFit.val = Float.MAX_VALUE;

		for(int i = 0; i < freeRectangles.size(); ++i)
		{
			float areaFit = freeRectangles.get(i).width() * freeRectangles.get(i).height() - width * height;

			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width() >= width && freeRectangles.get(i).height() >= height)
			{
				float leftoverHoriz = Helper.abs(freeRectangles.get(i).width() - width);
				float leftoverVert = Helper.abs(freeRectangles.get(i).height() - height);
				float shortSideFit = Helper.min(leftoverHoriz, leftoverVert);

				if (areaFit < bestAreaFit.val || (areaFit == bestAreaFit.val && shortSideFit < bestShortSideFit.val))
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							width,
							height
					);
					bestShortSideFit.val = shortSideFit;
					bestAreaFit.val = areaFit;
				}
			}

			if (freeRectangles.get(i).width() >= height && freeRectangles.get(i).height() >= width)
			{
				float leftoverHoriz = Helper.abs(freeRectangles.get(i).width() - height);
				float leftoverVert = Helper.abs(freeRectangles.get(i).height() - width);
				float shortSideFit = Helper.min(leftoverHoriz, leftoverVert);

				if (areaFit < bestAreaFit.val || (areaFit == bestAreaFit.val && shortSideFit < bestShortSideFit.val))
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							height,
							width
					);
					bestShortSideFit.val = shortSideFit;
					bestAreaFit.val = areaFit;
				}
			}
		}
		return bestNode;	
	};
	
	
	/**
	 * 
	 * @param width
	 * @param height
	 * @param bestContactScore
	 * @return
	 */
	private Rect FindPositionForNewNodeContactPoint(float width, float height, outFloat bestContactScore) { 
		Rect bestNode = new Rect();
		// memset(&bestNode, 0, sizeof(Rect));

		bestContactScore.val = -1;

		for(int i = 0; i < freeRectangles.size(); ++i)
		{
			// Try to place the rectangle in upright (non-flipped) orientation.
			if (freeRectangles.get(i).width() >= width && freeRectangles.get(i).height() >= height)
			{
				int score = ContactPointScoreNode(freeRectangles.get(i).x(), freeRectangles.get(i).y(), width, height);
				if (score > bestContactScore.val)
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							width,
							height
					);
					bestContactScore.val = score;
				}
			}
			if (freeRectangles.get(i).width() >= height && freeRectangles.get(i).height() >= width)
			{
				int score = ContactPointScoreNode(freeRectangles.get(i).x(), freeRectangles.get(i).y(), height, width);
				if (score > bestContactScore.val)
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							height,
							width
					);
					bestContactScore.val = score;
				}
			}
		}
		return bestNode;
	};

	
	
	/**
	 * @param freeNode
	 * @param usedNode
	 * @return true if the free node was split.
	 */
	private boolean SplitFreeNode(Rect freeNode, Rect usedNode) { 
		// Test with SAT if the rectangles even intersect.
		if (usedNode.x() >= freeNode.x() + freeNode.width() || usedNode.x() + usedNode.width() <= freeNode.x() ||
			usedNode.y() >= freeNode.y() + freeNode.height() || usedNode.y() + usedNode.height() <= freeNode.y()) {
			return false;
		}
		
		/*
		System.out.printf("\nBefore SPLIT:\n");
		for (Rect r : freeRectangles)
			System.out.printf("    Free Rect:  X: %d    Y: %d     H: %d     W: %d\n", r.x, r.y, r.height, r.width);
		*/

		
		if (usedNode.x() < freeNode.x() + freeNode.width() && usedNode.x() + usedNode.width() > freeNode.x())
		{
			// New node at the top side of the used node.
			if (usedNode.y() > freeNode.y() && usedNode.y() < freeNode.y() + freeNode.height())
			{
				Rect newNode = new Rect(
						freeNode.x(),
						freeNode.y(),
						freeNode.width(),
						usedNode.y() - freeNode.y()
						);
				freeRectangles.add(newNode);
			}

			// New node at the bottom side of the used node.
			if (usedNode.y() + usedNode.height() < freeNode.y() + freeNode.height())
			{
				Rect newNode = new Rect(
						freeNode.x(),
						usedNode.y() + usedNode.height(),
						freeNode.width(),
						freeNode.y() + freeNode.height() - (usedNode.y() + usedNode.height())
						);
				freeRectangles.add(newNode);
			}
		}

		if (usedNode.y() < freeNode.y() + freeNode.height() && usedNode.y() + usedNode.height() > freeNode.y())
		{
			// New node at the left side of the used node.
			if (usedNode.x() > freeNode.x() && usedNode.x() < freeNode.x() + freeNode.width())
			{
				Rect newNode = new Rect(
						freeNode.x(),
						freeNode.y(),
						usedNode.x() - freeNode.x(),
						freeNode.height()
					);
				freeRectangles.add(newNode);
			}

			// New node at the right side of the used node.
			if (usedNode.x() + usedNode.width() < freeNode.x() + freeNode.width())
			{
				Rect newNode = new Rect(
						usedNode.x() + usedNode.width(),
						freeNode.y(),
						freeNode.x() + freeNode.width() - (usedNode.x() + usedNode.width()),
						freeNode.height()
					);
				freeRectangles.add(newNode);
			}
		}
		/*
		System.out.printf("\nAfter SPLIT:\n");
		for (Rect r : freeRectangles)
			System.out.printf("    Free Rect:  X: %d    Y: %d     H: %d     W: %d\n", r.x, r.y, r.height, r.width);
		*/
		return true;

	};

	
	/** 
	 * Goes through the free rectangle list and removes any redundant entries.
	 */
	private void PruneFreeList() {

		for(int i = 0; i < freeRectangles.size(); ++i)
			for(int j = i+1; j < freeRectangles.size(); ++j) 
			{
				if (Rect.isContainedIn(freeRectangles.get(i), freeRectangles.get(j)))
				{
					freeRectangles.remove(i);
					--i;
					break;
				}
				if (Rect.isContainedIn(freeRectangles.get(j), freeRectangles.get(i)))
				{
					freeRectangles.remove(j);
					--j;
				}
			}
	}
}
