package com.fox.brian.binpack.algorithms;

import java.util.ArrayList;
import java.util.List;


import com.fox.brian.binpack.Bin;
import com.fox.brian.binpack.porthacks.outInt;
import com.fox.brian.binpack.util.Helper;
import com.fox.brian.binpack.util.Rect;

public class GuillotineContainer<T> extends Container<T> {

	
	
	/**
	 * Stores a list of all the rectangles that we have packed so far. This 
	 * is used only to compute the Occupancy ratio, so if you want to have 
	 * the packer consume less memory, this can be removed.
	 */
	private ArrayList<Rect> usedRectangles;

	
	/**
	 * Stores a list of rectangles that represents the free area of the bin. 
	 * This rectangles in this list are disjoint.
	 */
	private ArrayList<Rect> freeRectangles;
	
	
//	@SuppressWarnings("unused")
//	private DisjointRectCollection disjointRects;

	
	
	
	public GuillotineContainer() {
		usedRectangles = new ArrayList<Rect>();
		freeRectangles = new ArrayList<Rect>();
		bins = new ArrayList<Bin<T>>();
//		disjointRects = new DisjointRectCollection();
	}
	


	public GuillotineContainer(
			float maxWidth, 
			float maxHeight, 
			float minWidth,
			float minHeight) {
		
		this();
		
		binWidth = maxWidth;
		binHeight = maxHeight;

		// [NOT PORTED]
		// #ifdef _DEBUG
		// 	disjointRects.Clear();
		// #endif

		// Clear any memory of previously packed rectangles.
		usedRectangles.clear();

		// We start with a single big free rectangle that spans the whole bin.
		Rect n = new Rect(0,0, binWidth, binHeight);

		freeRectangles.clear();
		freeRectangles.add(n);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Specifies the different choice heuristics that can be used when 
	 * deciding which of the free subrectangles to place the to-be-packed 
	 * rectangle into.
	 */
	public enum FreeRectChoiceHeuristic {
		RectBestAreaFit,
		RectBestShortSideFit,
		RectBestLongSideFit,
		RectWorstAreaFit,
		RectWorstShortSideFit,
		RectWorstLongSideFit
	}

	
	/** Specifies the different choice heuristics that can be used when 
	 * the packer needs to decide whether to subdivide the remaining free 
	 * space in horizontal or vertical direction.
	 */
	public enum GuillotineSplitHeuristic
	{
		SplitShorterLeftoverAxis, ///< -SLAS
		SplitLongerLeftoverAxis, ///< -LLAS
		SplitMinimizeArea, ///< -MINAS, Try to make a single big rectangle at the expense of making the other small.
		SplitMaximizeArea, ///< -MAXAS, Try to make both remaining rectangles as even-sized as possible.
		SplitShorterAxis, ///< -SAS
		SplitLongerAxis ///< -LAS
	};

	

	/** 
	 * Inserts a single rectangle into the bin. The packer might rotate the 
	 * rectangle, in which case the returned struct will have the width and 
	 * height values swapped.
	 * 
	 * @param width
	 * @param height
	 * @param merge
	 * 			If true, performs free Rectangle Merge procedure after 
	 * 			packing the new rectangle. This procedure tries to 
	 * 			defragment the list of disjoint free rectangles to improve 
	 * 			packing performance, but also takes up some extra time.
	 * @param rectChoice
	 *          The free rectangle choice heuristic rule to use.
	 * @param splitMethod
	 *          The free rectangle split heuristic rule to use.
	 * @return
	 */
	public Rect insert(
//			float width, 
//			float height, 
			Bin<T> bin,
			boolean merge, 
			FreeRectChoiceHeuristic rectChoice, 
			GuillotineSplitHeuristic splitMethod
			) { 

		// Find where to put the new rectangle.
		outInt out = new outInt();
		Rect newRect = FindPositionForNewNode(bin.getWidth(), bin.getHeight(), rectChoice, out);
		int freeNodeIndex = out.val;

		// Abort if we didn't have enough space in the bin.
		if (newRect.height() == 0) {
			overflow = true;
			return newRect;
		}
			
		// Remove the space that was just consumed by the new rectangle.
		splitFreeRectByHeuristic(freeRectangles.get(freeNodeIndex), newRect, splitMethod);
		// freeRectangles.erase(freeRectangles.begin() + freeNodeIndex);
		freeRectangles.remove(freeNodeIndex);

		// Perform a Rectangle Merge step if desired.
		if (merge)
			mergeFreeList();

		// Remember the new used rectangle.
		usedRectangles.add(newRect);

		// Check that we're really producing correct packings here.
		// [NOT PORTED] debug_assert(disjointRects.add(newRect) == true);

		Bin<T> newBin = new Bin<T>(bin, newRect.x(), newRect.y());
		bins.add(newBin);
		return newRect;
	}

	/// Inserts a list of rectangles into the bin.
	/// @param rects The list of rectangles to add. This list will be destroyed in the packing process.
	/// @param dst The output as a list of rectangles. Note that the indices will not correspond to the input indices.
	/// @param merge If true, performs Rectangle Merge operations during the packing process.
	/// @param rectChoice The free rectangle choice heuristic rule to use.
	/// @param splitMethod The free rectangle split heuristic rule to use.
	/*
	void insert(
			ArrayList<Rect> rects, 
			ArrayList<Rect> dst, 
			boolean merge, 
			FreeRectChoiceHeuristic rectChoice, 
			GuillotineSplitHeuristic splitMethod
			) {
		
		dst.clear();

		// Remember variables about the best packing choice we have made so far during the iteration process.
		int bestFreeRect = 0;
		int bestRect = 0;
		boolean bestFlipped = false;

		// Pack rectangles one at a time until we have cleared the rects array of all rectangles.
		// rects will get destroyed in the process.
		while(rects.size() > 0)
		{
			// Stores the penalty score of the best rectangle placement - bigger=worse, smaller=better.
			float bestScore = Float.POSITIVE_INFINITY;

			for(int i = 0; i < freeRectangles.size(); ++i)
			{
				for(int j = 0; j < rects.size(); ++j)
				{
					// If this rectangle is a perfect match, we pick it instantly.
					if (rects.get(j).width() == freeRectangles.get(i).width() 
							&& rects.get(j).height() == freeRectangles.get(i).height())
					{
						bestFreeRect = i;
						bestRect = j;
						bestFlipped = false;
						bestScore = Float.NEGATIVE_INFINITY;
						i = freeRectangles.size(); // Force a jump out of the outer loop as well - we got an instant fit.
						break;
					}
					// If flipping this rectangle is a perfect match, pick that then.
					else if (rects.get(j).height() == freeRectangles.get(i).width() 
							&& rects.get(j).width() == freeRectangles.get(i).height())
					{
						bestFreeRect = i;
						bestRect = j;
						bestFlipped = true;
						bestScore = Float.NEGATIVE_INFINITY;
						i = freeRectangles.size(); // Force a jump out of the outer loop as well - we got an instant fit.
						break;
					}
					// Try if we can fit the rectangle upright.
					else if (rects.get(j).width() <= freeRectangles.get(i).width() 
							&& rects.get(j).height() <= freeRectangles.get(i).height())
					{
						float score = ScoreByHeuristic(rects.get(i).width(), rects.get(i).height(), 
								freeRectangles.get(i), rectChoice);
						if (score < bestScore)
						{
							bestFreeRect = i;
							bestRect = j;
							bestFlipped = false;
							bestScore = score;
						}
					}
					// If not, then perhaps flipping sideways will make it fit?
					else if (rects.get(i).height() <= freeRectangles.get(i).width() 
							&& rects.get(i).width() <= freeRectangles.get(i).height())
					{
						float score = ScoreByHeuristic(
								rects.get(i).height(), 
								rects.get(i).width(), 
								freeRectangles.get(i), 
								rectChoice
								);
						if (score < bestScore)
						{
							bestFreeRect = i;
							bestRect = j;
							bestFlipped = true;
							bestScore = score;
						}
					}
				}
			}

			// If we didn't manage to find any rectangle to pack, abort.
			if (bestScore == Float.MAX_VALUE)
				return;

			// Otherwise, we're good to go and do the actual packing.
			float tempheight = rects.get(bestRect).height();
			float tempwidth = rects.get(bestRect).width();
			if (bestFlipped) {
				tempheight = rects.get(bestRect).width();
				tempwidth = rects.get(bestRect).height();
				System.out.println("bestFlipped");
			}

			Rect newNode = new Rect(
				freeRectangles.get(bestFreeRect).x(),
				freeRectangles.get(bestFreeRect).y(),
				tempwidth,
				tempheight
			);


			// Remove the free space we lost in the bin.
			splitFreeRectByHeuristic(freeRectangles.get(bestFreeRect), newNode, splitMethod);
			// freeRectangles.erase(freeRectangles.begin() + bestFreeRect);
			freeRectangles.remove(bestFreeRect);
			
			// Remove the rectangle we just packed from the input list.
			// rects.erase(rects.begin() + bestRect);
			freeRectangles.remove(bestRect);

			// Perform a Rectangle Merge step if desired.
			if (merge)
				mergeFreeList();

			// Remember the new used rectangle.
			usedRectangles.add(newNode);

			// Check that we're really producing correct packings here.
			// [NOT PORTED] 
			// debug_assert(disjointRects.Add(newNode) == true);
		}

		
	}
	*/

// Implements GUILLOTINE-MAXFITTING, an experimental heuristic that's really cool but didn't quite work in practice.
//	void InsertMaxFitting(std::vector<RectSize> &rects, std::vector<Rect> &dst, bool merge, 
//		FreeRectChoiceHeuristic rectChoice, GuillotineSplitHeuristic splitMethod);

	/**
	 * Computes the ratio of used/total surface area. 0.00 means no space is 
	 * yet used, 1.00 means the whole bin is used.
	 * 
	 * @return
	 */
	public float occupancy() {
		// TODO float Occupancy() const;
		///\todo The occupancy rate could be cached/tracked incrementally instead
		///      of looping through the list of packed rectangles here.
		long usedSurfaceArea = 0;
		for(int i = 0; i < usedRectangles.size(); ++i)
			usedSurfaceArea += usedRectangles.get(i).width() * usedRectangles.get(i).height();
		return usedSurfaceArea / (binWidth * binHeight);
	};

	
	/**
	 * Returns the internal list of disjoint rectangles that track the free 
	 * area of the bin. You may alter this vector any way desired, as long 
	 * as the end result still is a list of disjoint rectangles.
	 *
	 * @return a list of disjoint rectangles
	 */
	ArrayList<Rect> getFreeRectangles() { 
		return freeRectangles; 
	}

	
	/**
	 *  Returns the list of packed rectangles. You may alter this vector at 
	 *  will, for example, you can move a Rect from this list to the Free 
	 *  Rectangles list to free up space on-the-fly, but notice that this 
	 *  causes fragmentation.
	 *  @return list of packed rectangles
	 */
	List<Rect> getUsedRectangles() { 
		return usedRectangles; 
	}

	/** 
	 * Performs a Rectangle Merge operation. This procedure looks for 
	 * adjacent free rectangles and merges them if they can be represented 
	 * with a single rectangle. Takes up Theta(|freeRectangles|^2) time.
	 */
	void mergeFreeList() {
		/* 
		[NOT PORTED]
		#ifdef _DEBUG
		DisjointRectCollection test;
		for(size_t i = 0; i < freeRectangles.size(); ++i)
			assert(test.Add(freeRectangles.get(i)) == true);
		#endif
		*/

		// Do a Theta(n^2) loop to see if any pair of free rectangles could 
		// me merged into one.  Note that we miss any opportunities to merge 
		// three rectangles into one. (should call this function again to 
		// detect that)
		for(int i = 0; i < freeRectangles.size(); ++i)
			for(int j = i+1; j < freeRectangles.size(); ++j)
			{
				if (freeRectangles.get(i).width() == freeRectangles.get(j).width() 
						&& freeRectangles.get(i).x() == freeRectangles.get(j).x())
				{
					if (freeRectangles.get(i).y() == 
							freeRectangles.get(j).y() + freeRectangles.get(j).height())
					{
						Rect oldie = freeRectangles.get(i);
						Rect newbie = new Rect (
								oldie.x(), 
								oldie.y() - freeRectangles.get(j).height(), 
								oldie.width(), 
								oldie.height() + freeRectangles.get(j).height()
						);
							
						
						freeRectangles.set(i, newbie);
						freeRectangles.remove(j);
						--j;
					}
					else if (freeRectangles.get(i).y() + freeRectangles.get(i).height() 
							== freeRectangles.get(j).y())
					{
						Rect oldie = freeRectangles.get(i);
						Rect newbie = new Rect (
								oldie.x(), 
								oldie.y(), 
								oldie.width(), 
								oldie.height() + freeRectangles.get(j).height()
						);
						freeRectangles.set(i, newbie);
						freeRectangles.remove(j);
						--j;
					}
				}
				else if (freeRectangles.get(i).height() == freeRectangles.get(j).height() 
						&& freeRectangles.get(i).y() == freeRectangles.get(j).y())
				{
					if (freeRectangles.get(i).x() == 
							freeRectangles.get(j).x() + freeRectangles.get(j).width())
					{
						Rect oldie = freeRectangles.get(i);
						Rect newbie = new Rect (
								oldie.x() - freeRectangles.get(j).width(), 
								oldie.y(), 
								oldie.width() + freeRectangles.get(j).width(), 
								oldie.height()
						);
						freeRectangles.set(i, newbie);
						freeRectangles.remove(j);
						--j;
					}
					else if (freeRectangles.get(i).x() + freeRectangles.get(i).width() 
							== freeRectangles.get(j).x())
					{
						Rect oldie = freeRectangles.get(i);
						Rect newbie = new Rect (
								oldie.x(), 
								oldie.y(), 
								oldie.width() + freeRectangles.get(j).width(), 
								oldie.height()
						);
						freeRectangles.set(i, newbie);
						freeRectangles.remove(j);
						--j;
					}
				}
			}
		/* 
		[NOT PORTED]

		#ifdef _DEBUG
			test.Clear();
			for(size_t i = 0; i < freeRectangles.size(); ++i)
				assert(test.Add(freeRectangles.get(i)) == true);
		#endif
		*/
		
		
	}


	

	/*
	#ifdef _DEBUG
		/// Used to track that the packer produces proper packings.
		DisjointRectCollection disjointRects;
	#endif
    */

	/** 
	 * Goes through the list of free rectangles and finds the best one to place 
	 * a rectangle of given size into.  Running time is Theta(|freeRectangles|).
	 * @param width
	 * @param height
	 * @param rectChoice
	 * @param nodeIndex [out] the index of the free rectangle in the 
	 *        freeRectangles array into which the new rect was placed.
	 * @return a Rect structure that represents the placement of the new rect 
	 *         into the best free rectangle.
	 */
	// 	Rect FindPositionForNewNode(int width, int height, FreeRectChoiceHeuristic rectChoice, int *nodeIndex) {}
	Rect FindPositionForNewNode(float width, float height, FreeRectChoiceHeuristic rectChoice, outInt nodeIndex) { 
		Rect bestNode = new Rect();
		
		// [NOT PORTED] memset(&bestNode, 0, sizeof(Rect));

		float bestScore = Float.POSITIVE_INFINITY;

		/// Try each free rectangle to find the best one for placement.
		for(int i = 0; i < freeRectangles.size(); ++i)
		{
			// If this is a perfect fit upright, choose it immediately.
			if (width == freeRectangles.get(i).width() && height == freeRectangles.get(i).height())
			{
				bestNode = new Rect(
						freeRectangles.get(i).x(),
						freeRectangles.get(i).y(),
						width,
						height
					);
				bestScore = Float.NEGATIVE_INFINITY;
				nodeIndex.val = i;
				// [NOT PORTED] debug_assert(disjointRects.disjoint(bestNode));
				break;
			}
			// If this is a perfect fit sideways, choose it.
			else if (allowRotation && height == freeRectangles.get(i).width() && width == freeRectangles.get(i).height())
			{
				bestNode = new Rect(
						freeRectangles.get(i).x(),
						freeRectangles.get(i).y(),
						height,
						width
					);

				bestScore = Float.NEGATIVE_INFINITY;
				nodeIndex.val = i;
				// [NOT PORTED] debug_assert(disjointRects.disjoint(bestNode));
				break;
			}
			// Does the rectangle fit upright?
			else if (width <= freeRectangles.get(i).width() && height <= freeRectangles.get(i).height())
			{
				float score = ScoreByHeuristic(width, height, freeRectangles.get(i), rectChoice);

				if (score < bestScore)
				{
					bestNode = new Rect(
							freeRectangles.get(i).x(),
							freeRectangles.get(i).y(),
							width,
							height
						);
					bestScore = score;
					nodeIndex.val = i;
					// [NOT PORTED] debug_assert(disjointRects.disjoint(bestNode));
				}
			}
			// Does the rectangle fit sideways?
			else if (allowRotation && height <= freeRectangles.get(i).width() && width <= freeRectangles.get(i).height())
			{
				float score = ScoreByHeuristic(height, width, freeRectangles.get(i), rectChoice);

				if (score < bestScore)
				{
					bestNode = new Rect(
						freeRectangles.get(i).x(),
						freeRectangles.get(i).y(),
						height,
						width
					);
					bestScore = score;
					nodeIndex.val = i;
					// [NOT PORTED] debug_assert(disjointRects.disjoint(bestNode));
				}
			}
		}
		return bestNode;
	
	}

	// static int ScoreByHeuristic(int width, int height, const Rect &freeRect, FreeRectChoiceHeuristic rectChoice);
	static float ScoreByHeuristic(float width, float height, Rect freeRect, FreeRectChoiceHeuristic rectChoice) { 
		switch(rectChoice)
		{
			case RectBestAreaFit: 
				return ScoreBestAreaFit(width, height, freeRect);
			case RectBestShortSideFit: 
				return ScoreBestShortSideFit(width, height, freeRect);
			case RectBestLongSideFit: 
				return ScoreBestLongSideFit(width, height, freeRect);
			case RectWorstAreaFit: 
				return ScoreWorstAreaFit(width, height, freeRect);
			case RectWorstShortSideFit: 
				return ScoreWorstShortSideFit(width, height, freeRect);
			case RectWorstLongSideFit: 
				return ScoreWorstLongSideFit(width, height, freeRect);
			default: 
				assert(false); 
				return Float.POSITIVE_INFINITY;
		}
	}
	
	/* 
	 * The following functions compute (penalty) score values if a rect of 
	 * the given size was placed into the given free rectangle. In these 
	 * score values, smaller is better.
	 */
	static float ScoreBestAreaFit(float width, float height, Rect freeRect) {
		return freeRect.width() * freeRect.height() - width * height;
	}
	
	static float ScoreBestShortSideFit(float width, float height, Rect freeRect) {
		float leftoverHoriz = Helper.abs(freeRect.width() - width);
		float leftoverVert = Helper.abs(freeRect.height() - height);
		float leftover = Helper.min(leftoverHoriz, leftoverVert);
		return leftover;
	}
	
	static float ScoreBestLongSideFit(float width, float height, Rect freeRect) {
		float leftoverHoriz = Helper.abs(freeRect.width() - width);
		float leftoverVert = Helper.abs(freeRect.height() - height);
		float leftover = Helper.max(leftoverHoriz, leftoverVert);
		return leftover;
	}

	static float ScoreWorstAreaFit(float width, float height, Rect freeRect) {
		return -ScoreBestAreaFit(width, height, freeRect);
	}
	
	static float ScoreWorstShortSideFit(float width, float height, Rect freeRect) {
		return -ScoreBestShortSideFit(width, height, freeRect);
	}
	
	static float ScoreWorstLongSideFit(float width, float height, Rect freeRect) {
		return -ScoreBestLongSideFit(width, height, freeRect);
	}

	/**
	 * Splits the given L-shaped free rectangle into two new free rectangles 
	 * after placedRect has been placed into it.  Determines the split axis 
	 * by using the given heuristic.
	 * 
	 * @param freeRect
	 * @param placedRect
	 * @param method
	 */
	// void SplitFreeRectByHeuristic(const Rect &freeRect, const Rect &placedRect, GuillotineSplitHeuristic method);
	void splitFreeRectByHeuristic(Rect freeRect, Rect placedRect, GuillotineSplitHeuristic method) {
		// Compute the lengths of the leftover area.
		float w = freeRect.width() - placedRect.width();
		float h = freeRect.height() - placedRect.height();

		// Placing placedRect into freeRect results in an L-shaped free area, which must be split into
		// two disjoint rectangles. This can be achieved with by splitting the L-shape using a single line.
		// We have two choices: horizontal or vertical.	

		// Use the given heuristic to decide which choice to make.

		boolean splitHorizontal;
		switch(method)
		{
			case SplitShorterLeftoverAxis:
				// Split along the shorter leftover axis.
				splitHorizontal = (w <= h);
				break;
			case SplitLongerLeftoverAxis:
				// Split along the longer leftover axis.
				splitHorizontal = (w > h);
				break;
			case SplitMinimizeArea:
				// Maximize the larger area == minimize the smaller area.
				// Tries to make the single bigger rectangle.
				splitHorizontal = (placedRect.width() * h > w * placedRect.height());
				break;
			case SplitMaximizeArea:
				// Maximize the smaller area == minimize the larger area.
				// Tries to make the rectangles more even-sized.
				splitHorizontal = (placedRect.width() * h <= w * placedRect.height());
				break;
			case SplitShorterAxis:
				// Split along the shorter total axis.
				splitHorizontal = (freeRect.width() <= freeRect.height());
				break;
			case SplitLongerAxis:
				// Split along the longer total axis.
				splitHorizontal = (freeRect.width() > freeRect.height());
				break;
			default:
				splitHorizontal = true;
				assert(false);
		}

		// Perform the actual split.
		splitFreeRectAlongAxis(freeRect, placedRect, splitHorizontal);
		
		
		
	};

	/**
	 * Splits the given L-shaped free rectangle into two new free rectangles 
	 * along the given fixed split axis.
	 * 
	 * @param freeRect
	 * @param placedRect
	 * @param splitHorizontal
	 */
	// void splitFreeRectAlongAxis(const Rect &freeRect, const Rect &placedRect, bool splitHorizontal);
	void splitFreeRectAlongAxis(Rect freeRect, Rect placedRect, boolean splitHorizontal) {
		// Form the two new rectangles.
		float bottom_x = freeRect.x();
		float bottom_y = freeRect.y() + placedRect.height();
		float bottom_height = freeRect.height() - placedRect.height();
		float bottom_width = 0;
		
		float right_x = freeRect.x() + placedRect.width();
		float right_y = freeRect.y();
		float right_width = freeRect.width() - placedRect.width();
		float right_height = 0;
		
		if (splitHorizontal)
		{
			bottom_width = freeRect.width();
			right_height = placedRect.height();
		}
		else // Split vertically
		{
			bottom_width = placedRect.width();
			right_height = freeRect.height();
		}

		Rect bottom = new Rect(bottom_x, bottom_y, bottom_width, bottom_height);
		Rect right = new Rect(right_x, right_y, right_width, right_height);

		// Add the new rectangles into the free rectangle pool if they weren't degenerate.
		if (bottom.width() > 0 && bottom.height() > 0)
			freeRectangles.add(bottom);
		if (right.width() > 0 && right.height() > 0)
			freeRectangles.add(right);

		// [NOT PORTED]debug_assert(disjointRects.disjoint(bottom));
		// [NOT PORTED ]debug_assert(disjointRects.disjoint(right));

		
		
	}


}
