import swiftbot.*;

import java.util.Scanner;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

public class ZigZag {
	static SwiftBotAPI swiftBot;
	static final String RESET = "\u001B[0m";
	static final String CYAN = "\u001B[36m";
	static final String YELLOW = "\u001B[33m";
	static final String GREEN = "\u001B[32m";
	static final String WHITE = "\u001B[37m";
	static final String BOLD = "\u001B[1m";
	static final int fullRotateAt100 = 1450;
	static final double constantK = 0.3416667;
	static final int[][] colours = { { 255, 0, 0 }, // Red
			{ 0, 255, 0 }, // Green
			{ 0, 0, 255 }, // Blue
			{ 255, 255, 255 } // White
	};
	static double straightLineDistance;

	static Scanner scanner = new Scanner(System.in);
//settings
	static int minSpeed = 30;
	static int maxSpeed = 50;
	static int zigzagAngle = 40;
	static int trips = 1;
	static int journeyCount = 0;
	static double lastSectionLength = -1;
	static int lastZigzagCount = -1;
	static String decodedMessage;
	static double shortestJourney = -1;
	static int shortestAngle = -1;
	static double shortestSpeed = -1;
	static double shortestSection = -1;
	static int shortestNumOfZigzags = -1;
	static double longestJourney = -1;
	static int longestAngle = -1;
	static double longestSpeed = -1;
	static double longestSection = -1;
	static int longestNumOfZigzags = -1;
	static int speed;

	public static void main(String[] args) throws InterruptedException {
		swiftBot = SwiftBotAPI.INSTANCE;
		mainMenu();
	}
	public static void mainMenu() {
		swiftBot.disableAllButtons();

		System.out.println("=====ZIGZAG=====");
			swiftBot.disableAllButtons();
			System.out.println("Press Y to begin zigzag\nPress A to go to settings\nPress B to repeat last zigzag\nPress X to leave :(");
			// Y for zigzag
			swiftBot.enableButton(Button.Y, () -> {
				System.out.println("Executing zigzag, please enter a QR code");

				// resets the variable so if it fails 10 times it doesn't use old data
				decodedMessage = scanQR();

				double[] inputs = validateQR(decodedMessage);

				if (inputs == null) {
					System.out.println("Please scan a valid QR code.");
					mainMenu();
					return;
				}

				double sectionLength = inputs[0];
				int zigzagCount = (int) inputs[1];
				speed = generateRandomSpeed();
				// java doesn't check throws in lambdas
				try {
					runJourney(sectionLength, zigzagCount, speed);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mainMenu();
			});
			// A = settings
			swiftBot.enableButton(Button.A, () -> {
				try {
					enterSettings();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
			// B = repeat last zigzag
			swiftBot.enableButton(Button.B, () -> {
				if (lastSectionLength == -1) {
					System.out.println("No previous zigzag to repeat.");
					return;
				} else {
					System.out.println("Repeating last zigzag");
					swiftBot.disableButton(Button.B);
					// java doesn't check throws in lambdas
					try {
						runJourney(lastSectionLength, lastZigzagCount, speed);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				mainMenu();
			});
			swiftBot.enableButton(Button.X, () -> {
				summary();
				swiftBot.disableButton(Button.A);
				System.exit(0);
			});
		}

	

	public static int generateRandomSpeed() {

		Random rand = new Random();

		return rand.nextInt(maxSpeed - minSpeed + 1) + minSpeed;
	}

	public static void runJourney(double sectionLength, int zigzagCount, int speed) throws InterruptedException {

		System.out.println("Starting journey...");
		System.out.println("Speed: " + speed);
		System.out.println("Section length: " + sectionLength);
		System.out.println("ZigZag count: " + zigzagCount);
		System.out.println("Trips: " + trips);
		System.out.println("Speed range: " + minSpeed + "-" + maxSpeed);
		System.out.println("Zigzag angle: " + zigzagAngle);

		long startTime = System.currentTimeMillis();
		for(int i = 0; i < trips; i++){

		executeZigzag(sectionLength, zigzagCount, speed, false);
//do a 360, then turn around
		System.out.println("Zigzag complete, now doing 360");
		swiftBot.fillUnderlights(colours[0]);
		swiftBot.move(100, 100, fullRotateAt100);
		System.out.println("360 complete, now doing 180");
		swiftBot.move(100, 100, (fullRotateAt100 / 2));
		System.out.println("going now");

		System.out.println("Retracing journey...");

		executeZigzag(sectionLength, zigzagCount, speed, true);

		swiftBot.disableUnderlights();

		long endTime = System.currentTimeMillis();

		double duration = (endTime - startTime) / 1000.0;

		System.out.println("Journey complete. Duration: " + duration);

		saveToFile(duration);
		}
	}

	public static void promptNextAction() {

		System.out.println("Press Y for new journey or X to exit.");

		swiftBot.enableButton(Button.X, () -> {

			System.out.println("Exiting program.");
			System.exit(0);

		});

	}

	public static double[] validateQR(String qr) {

		if (qr == null || qr.isEmpty()) {
			System.out.println("Invalid QR code: no data found.");
			return null;
		}

		String[] data = qr.split("-");

		// must contain exactly two values
		if (data.length != 2) {
			System.out.println("Invalid QR format. Expected: length-count (example: 40-6)");
			return null;
		}

		try {

			double sectionLength = Double.parseDouble(data[0]);
			int zigzagCount = Integer.parseInt(data[1]);

			// section length validation
			if (sectionLength < 15 || sectionLength > 85) {
				System.out.println("Invalid section length. Must be between 15 and 85.");
				return null;
			}

			// zigzag count validation
			if (zigzagCount <= 0 || zigzagCount > 12 || zigzagCount % 2 != 0) {
				System.out.println("Invalid zigzag count. Must be an EVEN number between 2 and 12.");
				return null;
			}

			return new double[] { sectionLength, zigzagCount };

		} catch (NumberFormatException e) {

			System.out.println("Invalid QR code. Values must be numeric.");
			return null;
		}
	}

	public static void executeZigzag(double sectionLength, int zigzagCount, int speed, boolean returnTrip) {
		// fullRotate was calibrated at 100% speed, so you need to calculate time needed
		// for different degrees and speeds
		int turnTime = (int) (fullRotateAt100 * (zigzagAngle / 360.0) * (100.0 / speed));
		double velocity = speed * constantK;
		int travelTime = (int) ((sectionLength / velocity) * 1000);
		double startDistance;
		double endDistance;
		if (!returnTrip) {
			// take first distance measurement
			startDistance = swiftBot.useUltrasound();
			// One wheel is backwards on the robot
			// aims swiftbot the right way
			swiftBot.move(speed, speed, turnTime);
			for (int i = 0; i < zigzagCount; i++) {
				// if it's even
				if (i % 2 == 0) {
					// set to green
					swiftBot.fillUnderlights(colours[1]);
					swiftBot.move(speed, -speed, travelTime);
					swiftBot.move(-speed, -speed, turnTime * 2);
				} else {
					// if it's even
					// set to blue
					swiftBot.fillUnderlights(colours[2]);
					swiftBot.move(speed, -speed, travelTime);
					swiftBot.move(speed, speed, turnTime * 2);

				}
			}
			// straighten robot again for distance measurement
			swiftBot.move(speed, speed, turnTime);
			endDistance = swiftBot.useUltrasound();
		} else {
			// to properly retrace the path, you must reverse what you just did
			// take first distance measurement
			startDistance = swiftBot.useUltrasound();
			// One wheel is backwards on the robot
			// aims swiftbot the right way
			swiftBot.move(-speed, -speed, turnTime);
			for (int i = 0; i < zigzagCount; i++) {
				// if it's even
				if (i % 2 == 0) {
					// set to green
					swiftBot.fillUnderlights(colours[1]);
					swiftBot.move(speed, -speed, travelTime);
					swiftBot.move(speed, speed, turnTime * 2);
				} else {
					// if it's even
					// set to blue
					swiftBot.fillUnderlights(colours[2]);
					swiftBot.move(speed, -speed, travelTime);
					swiftBot.move(-speed, -speed, turnTime * 2);

				}
			}
			// straighten robot again for distance measurement
			swiftBot.move(-speed, -speed, (turnTime / 2));
			endDistance = swiftBot.useUltrasound();

		}
		lastSectionLength = sectionLength;
		lastZigzagCount = zigzagCount;
		journeyCount++;
		straightLineDistance = Math.abs(endDistance - startDistance);
		System.out.println("Straight line distance: " + straightLineDistance);
		if (longestJourney < straightLineDistance) {
			longestJourney = straightLineDistance;
			longestAngle = zigzagAngle;
			longestSpeed = speed * constantK;
			longestSection = sectionLength;
			longestNumOfZigzags = zigzagCount;
		} else if (shortestJourney == -1 || straightLineDistance < shortestJourney) {
			shortestJourney = straightLineDistance;
			shortestAngle = zigzagAngle;
			shortestSpeed = speed * constantK;
			shortestSection = sectionLength;
			shortestNumOfZigzags = zigzagCount;
		}

	}

	public static void saveToFile(double duration) {

		try {

			BufferedWriter writer = new BufferedWriter(new FileWriter("zigzag_log.txt", true));

			writer.write("===== JOURNEY " + journeyCount + " =====");
			writer.newLine();

			writer.write("Section Length: " + lastSectionLength);
			writer.newLine();

			writer.write("ZigZag Count: " + lastZigzagCount);
			writer.newLine();

			writer.write("ZigZag Angle: " + zigzagAngle);
			writer.newLine();

			writer.write("Speed Range: " + minSpeed + " - " + maxSpeed);
			writer.newLine();

			writer.write("Speed: " + speed);
			writer.newLine();

			writer.write("Duration: " + duration + " seconds");
			writer.newLine();

			writer.write("Timestamp: " + System.currentTimeMillis());
			writer.newLine();

			writer.write("Straight line distance " + straightLineDistance + "cm");// ???? idk the measurement yet
			writer.newLine();

			writer.write("-----------------------------");
			writer.newLine();
			writer.close();

			System.out.println("Journey data saved to zigzag_log.txt");

		} catch (IOException e) {

			System.out.println("Error writing to log file.");

		}
	}

	public static void summary() {
		System.out.println("Journeys completed: " + journeyCount);
		System.out.println("===Details of longest journey===");
		System.out.println("Straight line distance: " + longestJourney);
		System.out.println("Section length: " + longestSection);
		System.out.println("Zigzag count: " + longestNumOfZigzags);
		System.out.println("Zigzag Angle: " + longestAngle);
		System.out.println("Speed: " + longestSpeed + "m/s");
		System.out.println("===Details of shortest journey===");
		System.out.println("Straight line distance: " + shortestJourney);
		System.out.println("Section length: " + shortestSection);
		System.out.println("Zigzag count: " + shortestNumOfZigzags);
		System.out.println("Zigzag Angle: " + shortestAngle);
		System.out.println("Speed: " + shortestSpeed + "m/s");
		System.out.println("Location of log file: zigzag_log.txt");

	}

	public static void enterSettings() throws InterruptedException{
		swiftBot.disableAllButtons();

		System.out.println("=== SETTINGS ===");
		System.out.println("Press A to go back");
		System.out.println("Press B to adjust speed range");
		System.out.println("Press Y to adjust zigzag angle");
		System.out.println("Press X to adjust number of trips");

		swiftBot.enableButton(Button.A, () -> {
			System.out.println("Returning to main menu...");
			swiftBot.disableButton(Button.A);
			swiftBot.disableButton(Button.B);
			swiftBot.disableButton(Button.Y);
			swiftBot.disableButton(Button.X);
			mainMenu();
		});

		// SPEED RANGE
		swiftBot.enableButton(Button.B, () -> {

			System.out.println("Scan QR code with: minSpeed-maxSpeed");

			String qr = scanQR();

			if (qr == null) {
				try {
					enterSettings();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				return;
			}

			String[] values = qr.split("-");

			if (values.length != 2) {
				System.out.println("Invalid QR format. Use min-max");
				return;
			}

			try {

				int newMin = Integer.parseInt(values[0]);
				int newMax = Integer.parseInt(values[1]);

				if (newMin > 0 && newMax <= 100 && newMin < newMax) {

					minSpeed = newMin;
					maxSpeed = newMax;

					System.out.println("Speed range updated to: " + minSpeed + " - " + maxSpeed);

				} else {
					System.out.println("Invalid speed range.");
				}

			} catch (Exception e) {
				System.out.println("Invalid QR values.");
			}
			try {
				enterSettings();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		// ZIGZAG ANGLE
		swiftBot.enableButton(Button.Y, () -> {

			System.out.println("Scan QR code with new zigzag angle (0-360)");

			String qr = scanQR();

			if (qr == null) {
				try {
					enterSettings();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				return;
			}

			try {

				int newAngle = Integer.parseInt(qr);

				if (newAngle > 0 && newAngle <= 360) {

					zigzagAngle = newAngle;

					System.out.println("Zigzag angle updated to: " + zigzagAngle);

				} else {
					System.out.println("Invalid angle.");
				}

			} catch (Exception e) {
				System.out.println("Invalid QR value.");
			}
			try {
				enterSettings();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		// NUMBER OF TRIPS
		swiftBot.enableButton(Button.X, () -> {

			System.out.println("Scan QR code with number of trips (1-10)");

			String qr = scanQR();

			if (qr == null) {
				try {
					enterSettings();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}

			try {

				int newTrips = Integer.parseInt(qr);

				if (newTrips >= 1 && newTrips <= 10) {

					trips = newTrips;

					System.out.println("Trips updated to: " + trips);

				} else {
					System.out.println("Invalid trip value.");
				}

			} catch (Exception e) {
				System.out.println("Invalid QR value.");
			}
try {
	enterSettings();
} catch (InterruptedException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}
		});
		}

	public static String scanQR() {

		int attempts = 0;

		while (attempts < 15) {

			try {

				BufferedImage img = swiftBot.getQRImage();
				String message = swiftBot.decodeQRImage(img);

				if (!message.isEmpty()) {

					System.out.println("QR detected: " + message);
					return message;

				}else {
					System.out.println("No QR code found trying again! (attempt: " + attempts + ")");
				}

			} catch (Exception e) {
				System.out.println("Scanning failed...");
			}

			attempts++;
		}

		System.out.println("QR scan failed after 10 attempts.");

		return null;
	}
}
