package Pecinha;
import robocode.*;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;
import static robocode.util.Utils.normalRelativeAngleDegrees;
import java.awt.geom.Point2D;
import java.util.Random;
import java.awt.Color;

// API help : https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html

/**
 * PecinhaItsOver - a robot by (your name here)
 */
public class PecinhaItsOver extends AdvancedRobot
{
	private Random random = new Random();
	private enum Mode {
        caso1, caso2, caso3, caso4
    }
    private Mode currentMode = Mode.caso1;
	double moveAmount;
	int count = 0; // Keeps track of how long we've
	// been searching for our target
	double gunTurnAmt; // How much to turn our gun when searching
	String trackName; // Name of the robot we're currently tracking
	/**
	 * run: PecinhaItsOver's default behavior
	 */
	public void run() {
		// Initialization of the robot should be put here

		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:

		// setColors(Color.red,Color.blue,Color.green); // body,gun,radar
		setBodyColor(Color.BLACK);
		setGunColor(Color.WHITE);
		setRadarColor(Color.WHITE);
		setRadarColor(Color.BLACK);
		// Robot main loop
		trackName = null;
		setAdjustGunForRobotTurn(true); // mover a arma independentemente
		setAdjustRadarForGunTurn(true); // mover o radar independentemente
		gunTurnAmt = 10;
		moveAmount = Math.max(getBattleFieldWidth(), getBattleFieldHeight());
		while(true) {
			switch (currentMode) {
                case caso1:
                    stopAndGoMovement();
                    break;
                case caso2:
                    randomMovement();
                    break;
                case caso3:
                    spiralMovement();
                    break;
			}
			updateMovementMode();
			turnRadarRight(360);
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		double firePower = Math.min(500 / e.getDistance(), 3);
		double turnGunAmt = normalRelativeAngleDegrees(e.getBearing() + getHeading() - getGunHeading());
		double bulletSpeed = 20 - firePower * 3;
		long time = (long)(e.getDistance() / bulletSpeed);
		double absBearingDeg = (getHeading() + e.getBearing());
		if (absBearingDeg < 0) absBearingDeg += 360;
		double x = getX() + Math.sin(Math.toRadians(absBearingDeg)) * e.getDistance();
		double y = getY() + Math.cos(Math.toRadians(absBearingDeg)) * e.getDistance();
		double futureX = x + Math.sin(Math.toRadians(getHeading())) * getVelocity() * time;
		double futureY = y + Math.cos(Math.toRadians(getHeading())) * getVelocity() * time;
		double absDeg = absoluteBearing(getX(), getY(), futureX, futureY);
		setTurnGunRight(normalizeBearing(absDeg - getGunHeading()));
		if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
			setFire(firePower);
		}
		scan();
	}
	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		int turnDirection = random.nextBoolean() ? 90 : -90;
        	turnRight(turnDirection);
        	ahead(150);
        	turnRight(random.nextInt(180));
        	ahead(100);
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {
		back(50);
        setTurnRight(90); 
	}
	
	public void onHitRobot(HitRobotEvent e) {
		back(50);
		setTurnRight(90);
	}
	public double normalizeBearing(double angle) {
	while (angle >  180) angle -= 360;
	while (angle < -180) angle += 360;
	return angle;
	}
	double absoluteBearing(double x1, double y1, double x2, double y2) {
	double xo = x2-x1;
	double yo = y2-y1;
	double hyp = Point2D.distance(x1, y1, x2, y2);
	double arcSin = Math.toDegrees(Math.asin(xo / hyp));
	double bearing = 0;
	if (xo > 0 && yo > 0) { // both pos: lower-Left
		bearing = arcSin;
	} else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
		bearing = 360 + arcSin; // arcsin is negative here, actuall 360 - ang
	} else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
		bearing = 180 - arcSin;
	} else if (xo < 0 && yo < 0) { // both neg: upper-right
		bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
	}
	return bearing;
	}

	private void randomMovement() {
        double randomDistance = random.nextInt(200) - 100;
        setAhead(randomDistance);
        setTurnRight(random.nextInt(360)); // Randomize turn angle
    }
	private void spiralMovement() {
        double turnAngle = 30 + random.nextInt(30);
        setTurnRight(turnAngle);
        setAhead(100);
    }
	private void strafeMovement() {
        double strafeDistance = 30 + random.nextInt(60);
        setMaxVelocity(8);
        setAhead(strafeDistance);
        setTurnRight(random.nextInt(90));
    }
	private void stopAndGoMovement() {
        double moveDistance = 50;
        setMaxVelocity(8);
        setAhead(moveDistance);
        execute();
        setTurnRight(random.nextInt(90));
    }
		private void updateMovementMode() {
        double distanceToEnemy = getDistanceRemaining();
        if (distanceToEnemy < 100) {
            currentMode = Mode.caso1;
        }
        else if (distanceToEnemy > 200) {
            currentMode = Mode.caso2;
        }
        else {
            currentMode = Mode.caso3;
        }
    }
}

