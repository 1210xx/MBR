/**
 * @author Fan Min (minfanphd@163.com), Mei Zheng and Heng-Ru Zhang 
 */
package mbr;

import java.io.*;

public class MBR {
	public static final double DEFAULT_RATING = 3.0;
	/**
	 * The total number of numUsers
	 */
	private int numUsers;

	/**
	 * The total number of items
	 */
	private int numItems;

	/**
	 * The total number of ratings (non-zero values)
	 */
	private int numRatings;

	/**
	 * The predictions.
	 */
	private double[] predictions;

	/**
	 * Compressed rating matrix. User-item-rating triples.
	 */
	private int[][] compressedRatingMatrix;

	/**
	 * The degree of users (how many item he has rated).
	 */
	private int[] userDegrees;

	/**
	 * The average rating of the current user.
	 */
	private double[] userAverageRatings;

	/**
	 * The degree of users (how many item he has rated).
	 */
	private int[] itemDegrees;

	/**
	 * The average rating of the current item.
	 */
	private double[] itemAverageRatings;

	/**
	 * The first user start from 0. Let the first user has x ratings, the second
	 * user will start from x.
	 */
	private int[] userStartingIndices;

	/**
	 * Number of non-neighbor objects.
	 */
	private int numNonNeighbors;

	/**
	 * The radius (delta) for determining the neighborhood.
	 */
	private double radius;

	/**
	 ************************* 
	 * Construct the rating matrix.
	 * 
	 * @param paraRatingFilename
	 *            the rating filename.
	 * @param paraNumUsers
	 *            number of users
	 * @param paraNumItems
	 *            number of items
	 * @param paraNumRatings
	 *            number of ratings
	 ************************* 
	 */
	public MBR(String paraFilename, int paraNumUsers, int paraNumItems,
			int paraNumRatings) throws Exception {
		// Initialize these arrays
		numItems = paraNumItems;
		numUsers = paraNumUsers;
		numRatings = paraNumRatings;

		userDegrees = new int[numUsers];
		userStartingIndices = new int[numUsers + 1];
		userAverageRatings = new double[numUsers];
		itemDegrees = new int[numItems];
		compressedRatingMatrix = new int[numRatings][3];
		itemAverageRatings = new double[numItems];

		predictions = new double[numRatings];

		System.out.println("Reading " + paraFilename);
		// Step 2. Read the data file.
		File tempFile = new File(paraFilename);
		if (!tempFile.exists()) {
			System.out.println("File " + paraFilename + " does not exists.");
			System.exit(0);
		}// Of if
		BufferedReader tempBufReader = new BufferedReader(new FileReader(
				tempFile));
		String tempString;
		String[] tempStrArray;
		int tempIndex = 0;
		userStartingIndices[0] = 0;
		userStartingIndices[numUsers] = numRatings;
		while ((tempString = tempBufReader.readLine()) != null) {
			// Each line has three values
			tempStrArray = tempString.split(",");
			compressedRatingMatrix[tempIndex][0] = Integer
					.parseInt(tempStrArray[0]);
			compressedRatingMatrix[tempIndex][1] = Integer
					.parseInt(tempStrArray[1]);
			compressedRatingMatrix[tempIndex][2] = Integer
					.parseInt(tempStrArray[2]);

			userDegrees[compressedRatingMatrix[tempIndex][0]]++;
			itemDegrees[compressedRatingMatrix[tempIndex][1]]++;

			if (tempIndex > 0) {
				// Starting to read the data of a new user.
				if (compressedRatingMatrix[tempIndex][0] != compressedRatingMatrix[tempIndex - 1][0]) {
					userStartingIndices[compressedRatingMatrix[tempIndex][0]] = tempIndex;
				}// Of if
			}// Of if
			tempIndex++;
		}// Of while
		tempBufReader.close();

		double[] tempUserTotalScore = new double[numUsers];
		double[] tempItemTotalScore = new double[numItems];
		for (int i = 0; i < numRatings; i++) {
			tempUserTotalScore[compressedRatingMatrix[i][0]] += compressedRatingMatrix[i][2];
			tempItemTotalScore[compressedRatingMatrix[i][1]] += compressedRatingMatrix[i][2];
		}// Of for i

		for (int i = 0; i < numUsers; i++) {
			userAverageRatings[i] = tempUserTotalScore[i] / userDegrees[i];
		}// Of for i
		for (int i = 0; i < numItems; i++) {
			itemAverageRatings[i] = tempItemTotalScore[i] / itemDegrees[i];
		}// Of for i
	}// Of the first constructor

	/**
	 ************************* 
	 * Set the radius (delta).
	 * 
	 * @author Fan Min
	 ************************* 
	 */
	public void setRadius(double paraRadius) {
		if (paraRadius > 0) {
			radius = paraRadius;
		} else {
			radius = 0.1;
		}// Of if
	}// of setRadius

	/**
	 ************************* 
	 * Leave one out prediction. The predicted values are stored in predictions.
	 * 
	 * @see predictions
	 * @author Fan Min
	 ************************* 
	 */
	public void leaveOneOutPrediction() {
		double tempItemAverageRating;
		// Make each line of the code shorter.
		int tempUser, tempItem, tempRating;
		System.out.println("\r\nLeaveOneOutPrediction for radius " + radius);

		numNonNeighbors = 0;
		for (int i = 0; i < numRatings; i++) {
			tempUser = compressedRatingMatrix[i][0];
			tempItem = compressedRatingMatrix[i][1];
			tempRating = compressedRatingMatrix[i][2];

			// Step 1. Recompute average rating of the current item.
			tempItemAverageRating = (itemAverageRatings[tempItem]
					* itemDegrees[tempItem] - tempRating)
					/ (itemDegrees[tempItem] - 1);

			// Step 2. Recompute neighbors, at the same time obtain the ratings
			// of neighbors.
			int tempNeighbors = 0;
			double tempTotal = 0;
			int tempComparedItem;
			for (int j = userStartingIndices[tempUser]; j < userStartingIndices[tempUser + 1]; j++) {
				tempComparedItem = compressedRatingMatrix[j][1];
				if (tempItem == tempComparedItem) {
					continue;// Ignore itself.
				}// Of if

				if (Math.abs(tempItemAverageRating
						- itemAverageRatings[tempComparedItem]) < radius) {
					tempTotal += compressedRatingMatrix[j][2];
					tempNeighbors++;
				}// Of if
			}// Of for j

			if (tempNeighbors > 0) {
				predictions[i] = tempTotal / tempNeighbors;
			} else {
				predictions[i] = DEFAULT_RATING;
				numNonNeighbors++;
			}// Of if
			/*
			int tempRates = userStartingIndices[tempUser + 1]
					- userStartingIndices[tempUser];
			System.out.println("User " + tempUser + " rates " + tempRates
					+ " items. " + tempNeighbors
					+ " neighbors. itemAverageRating = "
					+ tempItemAverageRating + ", prediction = "
					+ predictions[i]);
					*/
		}// Of for i
	}// Of leaveOneOutPrediction

	/**
	 ************************* 
	 * Compute the MAE based on the deviation of each leave-one-out.
	 * 
	 * @author Fan Min
	 ************************* 
	 */
	public double computeMAE() throws Exception {
		double tempTotalError = 0;
		for (int i = 0; i < predictions.length; i++) {
			tempTotalError += Math.abs(predictions[i]
					- compressedRatingMatrix[i][2]);
		}// Of for i

		return tempTotalError / predictions.length;
	}// Of computeMAE

	/**
	 ************************* 
	 * Compute the MAE based on the deviation of each leave-one-out.
	 * 
	 * @author Fan Min
	 ************************* 
	 */
	public double computeRSME() throws Exception {
		double tempTotalError = 0;
		for (int i = 0; i < predictions.length; i++) {
			tempTotalError += (predictions[i] - compressedRatingMatrix[i][2])
					* (predictions[i] - compressedRatingMatrix[i][2]);
		}// Of for i

		double tempAverage = tempTotalError / predictions.length;

		return Math.sqrt(tempAverage);
	}// Of computeRSME

	/**
	 ************************* 
	 * The main entrance
	 * 
	 * @author Fan Min
	 ************************* 
	 */
	public static void main(String[] args) {
		try {
			// System.out.println("Main test 1");
			MBR tempRecommender = new MBR("src/data/movielens-943u1682m.txt",943, 1682, 100000);

			for (double tempRadius = 0.2; tempRadius < 0.6; tempRadius += 0.1) {
				tempRecommender.setRadius(tempRadius);

				tempRecommender.leaveOneOutPrediction();
				double tempMAE = tempRecommender.computeMAE();
				double tempRSME = tempRecommender.computeRSME();
				// double tempRSME = tempRecommender.computeRSME();

				System.out.println("Radius = " + tempRadius + ", MAE = "+ tempMAE + ", RSME = " + tempRSME+ ", numNonNeighbors = "+ tempRecommender.numNonNeighbors);
			}// Of for tempRadius
		} catch (Exception ee) {
			System.out.println(ee);
		}// of try
	}// of main

}// Of class MBR

