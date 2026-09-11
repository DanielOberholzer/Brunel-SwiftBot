import swiftbot.Button;
import swiftbot.SwiftBotAPI;

public class SwiftbotCalib {
	static SwiftBotAPI swiftbot = SwiftBotAPI.INSTANCE;
	final static int time = 2; // time in seconds
	final static int[] speedPercentages = { 30, 40, 50, 60, 70, 80, 90, 100 };
	static int i = 0;

public static void main(String[] args) {

System.out.println("Calibrate Example");
System.out.println("\nPlease place your robot at the start point, and press Button X to start moving.");
System.out.println("Ready to start moving at " + speedPercentages[i] + "% speed.");
swiftbot.enableButton(Button.X, () -> {
	double start = swiftbot.useUltrasound();
int currentSpeed = speedPercentages[i];
System.out.println("Moving at (" + currentSpeed + ", " +
currentSpeed + ")...");
swiftbot.move(currentSpeed, currentSpeed, time*1000);
i++;
double end = swiftbot.useUltrasound();
System.out.println(start - end);
if (i < speedPercentages.length) {
System.out.println("\nReady to start moving at " +
speedPercentages[i] + "% speed.");
} else {
System.out.println("\nFinished testing!");
swiftbot.disableButton(Button.X);
System.exit(0);
}
});
}
}
