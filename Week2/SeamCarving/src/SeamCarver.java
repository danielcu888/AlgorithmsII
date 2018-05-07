import edu.princeton.cs.algs4.Picture;

/*
 * A class to perform seam carving on a specified Picture.
 */
public class SeamCarver {
	
	// *************** PRIVATE DATA MEMBERS ***************
	
	// Current Picture
	private Picture picture;
	private boolean redraw = false;
	
	// Dimensions
	private int width;
	private int height;
	private int dim;
	
	// Arrays containing constant information
	private final int pixels[][];
	private final double energySq[][];
	
	// Constant information for terminal nodes
	private final int SOURCECOL;
	private final int TARGETCOL;
	
	// Cached arrays for SP finding.
	private double energySqTo[][];
	private int colTo[][];
	
	// Cached information on terminal Node for SP finding.
	private int colToTarget;
	private double energySqToTarget;
	
	private Orientation orientation = Orientation.PORTRAIT;
	
	// Static constants
	private static final double BORDERENERGYSQ = 1000.0*1000.0;

	
	// *************** PUBLIC STRUCTORS ***************
	
	/*
	 * @brief 1-parameter constructor.
	 * @param picture The specified picture used to
	 *   construct this SeamCarver object.
	 */
	public SeamCarver(Picture picture) {
		if (picture == null) {
			throw new IllegalArgumentException();
		}
		
		// Initialize data members
		
		// Dimensions
		this.width = picture.width();
		this.height = picture.height();
		this.dim = Math.max(this.width, this.height);
		
		
		// Picture
		this.picture = new Picture(this.width, this.height);
		
		
		// Transpositional arrays
		
		// RGB
		this.pixels = new int[this.dim][];
		
		// Energy
		this.energySq = new double[this.dim][];
		this.energySqTo = new double[this.dim][];
		
		// Paths
		this.colTo = new int[this.dim][];
		
		for (int row = 0; row < this.height; ++row) { // loop over rows
			
			// RGB
			this.pixels[row] = new int[this.dim];
			
			// Energy
			this.energySq[row] = new double[this.dim];
			this.energySqTo[row] = new double[this.dim];
		
			// Paths
			this.colTo[row] = new int[this.dim];
			
			for (int col = 0; col < this.width; ++col) { // loop over columns
			
				final int rgb = picture.getRGB(col, row);	

				// Picture (copy source)
				this.picture.setRGB(col, row, rgb);
				
				// RGB (copy picture)
				this.pixels[row][col] = rgb;
				
			}  // ! loop over rows
		} // ! loop over columns
		
		// Calculate energy
		for (int row = 0; row < this.height; ++row) {
			for (int col = 0; col < this.width; ++col) {		
				if (this.isBorderPixel(row, col)) {
					// Border pixels have fixed energy.
					this.energySq[row][col] = SeamCarver.BORDERENERGYSQ;
				} else {
					// Non-border pixels
					this.energySq[row][col] = this.energySq(row, col);
				}
			}
		}
				
		// Terminal Nodes
		
		// row and col of the source and target virtual nodes,
		// assigned values unachievable by regular pixels. They
		// never change throughout seam-carving.
		this.SOURCECOL = this.dim;
		this.TARGETCOL = this.dim+1;
		
		// Initialize cached path data.
		this.initializeCachedPathData();
	}	
	
	// *************** PRIVATE CLASSES ***************
	
	// Private enum to categorize picture orientation.
	private enum Orientation {
		PORTRAIT,
		LANDSCAPE
	}

	// *************** PRIVATE METHODS ***************
	
	// Helper function to (re-)initialize cached path data.
	private void initializeCachedPathData() {
		for (int row = 0; row < this.height; ++row) {
			for (int col = 0; col < this.width; ++col) {
				
				// EnergySqTo (all start as infinity).
				this.energySqTo[row][col] = Double.POSITIVE_INFINITY;
				
				// Path (Start with null paths).
				this.colTo[row][col] = col;	
			}
		}
		
		// Path to target node.
		this.colToTarget = this.TARGETCOL;
		this.energySqToTarget = Double.POSITIVE_INFINITY;
	}
	
	// Helper functions to calculate energy.
	
	private double deltaSqComponent(int rgb1, int rgb2) {
		final int mask = 0xFF;
		final int rdiff = (rgb1 >> 16 & mask) - (rgb2 >> 16 & mask);
		final int gdiff = (rgb1 >> 8 & mask) - (rgb2 >> 8 & mask);
		final int bdiff = (rgb1 & mask) - (rgb2 & mask);
		
		return rdiff*rdiff + gdiff*gdiff + bdiff*bdiff;		
	}
	
	private double deltaXSq(int row, int col) {
		final int lrgb = this.pixels[row][col-1];
		final int rrgb = this.pixels[row][col+1];
		return this.deltaSqComponent(lrgb, rrgb);
	}

	private double deltaYSq(int row, int col) {
		final int urgb = this.pixels[row-1][col];
		final int drgb = this.pixels[row+1][col];
		return this.deltaSqComponent(urgb, drgb);		
	}
	
	private double energySq(int row, int col) {
		if (this.isBorderPixel(row, col)) {
			return SeamCarver.BORDERENERGYSQ;
		}
		return this.deltaXSq(row, col) + this.deltaYSq(row, col);
	}
	
	private boolean isBorderPixel(int row, int col) {
		return row == 0 || row == this.height-1 ||
			   col == 0 || col == this.width-1;
	}

	// Helper functions to calculate column of adjacent pixels.
	
	private Integer middleAdj(int row, int col) {
		if (row < this.height-1) {
			return col;
		}
		
		return null;
	}

	private Integer leftAdj(int row, int col) {
		if (col > 0 && row < this.height-1) {
			return col-1;
		}
		
		return null;
	}

	private Integer rightAdj(int row, int col) {
		if (col < this.width-1 && row < this.height-1) {
			return col+1;
		}
		
		return null;
	}

	// Helper function to relax edges of a specified pixel.
	private void relax(int row, int col) {		
		final double newEnergySqTo = this.energySqTo[row][col] + this.energySq(row, col);
		
		if (row == this.height-1) {
			// Pixel is on bottom row, check if path to target is optimal.
			if (this.energySqToTarget > newEnergySqTo) {
				
				// Update path to target with most optimal hitherto.
				this.energySqToTarget = newEnergySqTo;
				this.colToTarget = col;
			}
		}
		else {
			// Check if paths to adjacent pixels are optimal.
			
			// Indices of adjacent pixels.
			final int adjRow = row + 1;
			final Integer[] adjCols = {this.leftAdj(row, col),
					                   this.middleAdj(row, col),
					                   this.rightAdj(row, col)};
	
			for (Integer adjCol : adjCols) { // Loop over adjacent pixels
				if (adjCol != null) {
					// Check if path to adjacent pixel is optimal.
					if (this.energySqTo[adjRow][adjCol] > newEnergySqTo) {
						// Update path to adjacent pixel with most optimal hitherto.
						this.energySqTo[adjRow][adjCol] = newEnergySqTo;
						this.colTo[adjRow][adjCol] = col;
					}
				}			
			}
		}
	}
	
	// Helper functions to transpose a square matrix in-place.
	// TODO - Convert to a generic function taking T[][]
	
	private void transpose(int[][] a) {
		for (int row = 0; row < this.dim; ++row) {
			for (int col = 0; col < this.dim; ++col) {
				final int tmp = a[row][col];
				a[row][col] = a[col][row];
				a[col][row] = tmp;
			}
		}
	}

	private void transpose(double[][] a) {
		for (int row = 0; row < this.dim; ++row) {
			for (int col = 0; col < this.dim; ++col) {
				final double tmp = a[row][col];
				a[row][col] = a[col][row];
				a[col][row] = tmp;
			}
		}
	}
	
	private void transpose() {
		
		// Transpose arrays.
		this.transpose(this.pixels);
		this.transpose(this.energySq);
		this.transpose(energySqTo);
		this.transpose(colTo);
		
		// Flip dimensions.
		final int tmp = this.width;
		this.width = this.height;
		this.height = tmp;
		
		// Flip orientation.
		this.orientation = (this.orientation == Orientation.PORTRAIT) 
						   ? Orientation.LANDSCAPE
						   : Orientation.PORTRAIT;
	}

	// Helper functions to validate pixel indices.
	
	private boolean validColumnIndex(int col) {
		return col >= 0 && col < this.width;
	}
	
	private boolean validRowIndex(int row) {
		return row >= 0 && row < this.height;
	}
	
	private boolean validIndices(int row, int col) {
		return this.validColumnIndex(col) && this.validRowIndex(row);
	}

	// Helper functions to shift array elements.
	// TODO - convert to generic function deleteCol(T[] a)
	
	private void deleteCol(int[] a, int col) {
		
		final int len = a.length;
		if (len == 0 || col == len-1) {
			// No shift necessary.
			return;
		} else {
			// Shift 'a' one column to the left from col+1
			// e.g., if a is {0,1,2,3,4}
			// and we wish to remove column 2 (containing 2)
			// Then we want to shift {3,4} over to column 2
			// System.arrayCopy(a, 2, 1, 3, 2), i.e., len = 5 - (2+1) = 5 - 3 = 2
			System.arraycopy(a, col+1, a, col, len-(col+1));
		}
	}

	private void deleteCol(double[] a, int col) {
		
		final int len = a.length;
		if (len == 0 || col == len-1) {
			// No shift necessary.
			return;
		} else {
			// Shift 'a' one column to the left from col+1
			System.arraycopy(a, col+1, a, col, len-(col+1));
		}
	}

	
	// *************** PUBLIC METHODS ***************
	
	/*
	 * @brief Return the current Picture associated with
	 *   this SeamCraver object.
	 * @return the Picture associated with
	 *   this SeamCraver object.
	 */
	public Picture picture() {
		if (this.redraw) {
			if (this.orientation != Orientation.PORTRAIT) {
				this.transpose();
			}

			this.picture = new Picture(this.width, this.height);
			
			for (int row = 0; row < this.height; ++row) {
				for (int col = 0; col < this.width; ++col) {
					this.picture.setRGB(col, row, this.pixels[row][col]);
				}
			}
			
			this.redraw = false;
		}
		
		return this.picture;
	}
	
	/*
	 * @brief Retrieve the width of the current
	 *   picture associated with this SeamCarver
	 *   object.
	 * @return the width (in pixels) of the current
	 *   picture associated with this SeamCarver
	 *   object.
	 */
	public int width() {
		return this.orientation == Orientation.PORTRAIT ? this.width : this.height;
	}
	
	/*
	 * @brief Retrieve the height of the current
	 *   picture associated with this SeamCarver
	 *   object.
	 * @return the height (in pixels) of the current
	 *   picture associated with this SeamCarver
	 *   object.
	 */
	public int height() {
		return this.orientation == Orientation.PORTRAIT ? this.height : this.width;
	}
	
	/*
	 * @brief Return the energy associated with the
	 *   pixel of the current picture, associated with
	 *   this SeamCarver object, at the specified
	 *   location.
	 * @param x The column index of the desired
	 *   pixel (0 is the left-most column).
	 * @param y The row index of the desired
	 *   pixel (0 is the top-most row).
	 * @return the energy associated with the
	 *   pixel of the current picture, associated with
	 *   this SeamCarver object, at the specified
	 *   location.
	 */
	public double energy(int col, int row) {
		if (!this.validIndices(row, col)) {
			throw new IllegalArgumentException();
		}
		
		final double eSq = this.orientation == Orientation.PORTRAIT
						   ? this.energySq[row][col]
						   : this.energySq[col][row];
				
		return Math.sqrt(eSq);
	}
	
	/*
	 * @brief Return the sequence of row indices
	 *   for the next horizontal seam of the current
	 *   picture associated with this SeamCarver
	 *   object.
	 * @return the sequence of row indices
	 *   for the next horizontal seam of the current
	 *   picture associated with this SeamCarver
	 *   object.
	 */
	public int[] findHorizontalSeam() {
		
		if (this.orientation == Orientation.PORTRAIT) {
			this.transpose();
		}
		
		final int[] seamRows = this.findVerticalSeam();		
		return seamRows;
	}
	
	/*
	 * @brief Return the sequence of column indices
	 *   for the next vertical seam of the current
	 *   picture associated with this SeamCarver
	 *   object.
	 * @return the sequence of column indices
	 *   for the next vertical seam of the current
	 *   picture associated with this SeamCarver
	 *   object.
	 */
	public int[] findVerticalSeam() {
		
		// Check if transpose is required.
		if (this.orientation != Orientation.PORTRAIT) {
			this.transpose();
		}

		// Relax edges directly connected to the source node (i.e., top row).
		for (int col = 0; col < this.width; ++col) {
			this.energySqTo[0][col] = 0.0; // Source node has zero energy.
			this.colTo[0][col] = this.SOURCECOL;
		}
	
		// Relax all remaining edges in topological order,
		// i.e., top row to bottom row.
		for (int row = 0; row < this.height; ++row) {
			for (int col = 0; col < this.width; ++col) {
				this.relax(row, col);
			}
		}
		
		// Construct seam.
		
		final int[] seamCols = new int[this.height];

		int row = this.height-1; // bottom row
		int col = this.colToTarget;
		do {
			seamCols[row] = col;
			
			col = this.colTo[row][col];
			--row;
		} while (row >= 0);
		
		return seamCols;
	}
	
	/*
	 * @brief Remove the next horizontal seam from
	 *   the current picture associated with this
	 *   SeamCarver object.
	 * @param seam The horizontal seam to be removed.
	 */
	public void removeHorizontalSeam(int[] seam) {
		
		// Check if transpose is necessary.
		if (this.orientation == Orientation.PORTRAIT) {
			this.transpose();
		}
		
		this.removeVerticalSeam(seam);
	}
	
	/*
	 * @brief Remove the next vertical seam from
	 *   the current picture associated with this
	 *   SeamCarver object.
	 * @param seam The vertical seam to be removed.
	 */
	public void removeVerticalSeam(int[] seam) {
		
		// Check if transpose is necessary.
		if (this.orientation != Orientation.PORTRAIT) {
			this.transpose();
		}

		// Defensive coding for argument validation.
		if (this.width() < 2) {
			throw new IllegalArgumentException();
		}
		
		if (seam == null) {
			throw new IllegalArgumentException();
		}

		final int length = seam.length;
		if (length != this.height()) {
			throw new IllegalArgumentException();
		}

		int lastColumnIdx = seam[0];
		for (int i = 0; i < length; ++i) {
			final int columnIdx = seam[i];
			if (!this.validColumnIndex(seam[i])) {
				throw new IllegalArgumentException();
			}
			
			if (Math.abs(lastColumnIdx - columnIdx) > 1) {				
				throw new IllegalArgumentException();
			} else {
				lastColumnIdx = columnIdx;
			}			
		}

		// Remove seam.
		for (int row = 0; row < this.height; ++row) {
			this.deleteCol(pixels[row], seam[row]);
			this.deleteCol(energySq[row], seam[row]);
		}
		
		// Update dimensions.
		
		--this.width;
		
		// Here update dim, so that when transposing,
		// we only transpose the section of the matrix
		// required, even though after we remove the first
		// seam they will be larger than the subsequent
		// values of dim.
		this.dim = Math.max(this.width, this.height);
		
		// Recalculate energies in columns (row, seam[row]-1)
		// and (row, seam[row] (which was previously seam[row]+1)),
		// all other energies are unaffected.
		for (int row = 0; row < this.height; ++row) {
			
			// Recalculate energy for pixel (row, seam[row]-1).
			if (this.validIndices(row, seam[row]-1)) {
				if (this.isBorderPixel(row, seam[row]-1)) {
					this.energySq[row][seam[row]-1] = SeamCarver.BORDERENERGYSQ;
				} else {
					this.energySq[row][seam[row]-1] = this.energySq(row, seam[row]-1);
				}
			}
			
			
			// Recalculate energy for pixel (row, seam[row]).
			if (this.validIndices(row, seam[row])) {
				if (this.isBorderPixel(row, seam[row])) {
					this.energySq[row][seam[row]] = SeamCarver.BORDERENERGYSQ;
				} else {
					this.energySq[row][seam[row]] = this.energySq(row, seam[row]);
				}
			}
		}

		// Reinitialize cached path data.
		this.initializeCachedPathData();
		
		// Set flag to regenerate picture on next request.
		this.redraw = true;		
	}
}
















