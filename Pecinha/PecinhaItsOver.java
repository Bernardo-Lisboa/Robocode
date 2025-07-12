package Pecinha;

import robocode.*;
import robocode.util.Utils;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

public class PecinhaItsOver extends AdvancedRobot {

    private static class EnemyWave {
        Point2D.Double fireLocation;
        long fireTime;
        double bulletVelocity;
        double directAngle;
        double distanceTraveled;
        double maxEscapeAngle;
        int direction;

        public static final int BINS = 31;
        public int[] guessFactors = new int[BINS];

        public EnemyWave(Point2D.Double fireLocation, long fireTime, double bulletVelocity, double directAngle, int direction) {
            this.fireLocation = fireLocation;
            this.fireTime = fireTime;
            this.bulletVelocity = bulletVelocity;
            this.directAngle = directAngle;
            this.direction = direction;
            this.maxEscapeAngle = Math.asin(Math.min(1.0, 8.0 / bulletVelocity)); // corrigido
        }

        public void update(long time) {
            distanceTraveled = (time - fireTime) * bulletVelocity;
        }

        public int getFactorIndex(Point2D.Double hitLocation) {
            double offsetAngle = Utils.normalRelativeAngle(Math.atan2(
                hitLocation.x - fireLocation.x,
                hitLocation.y - fireLocation.y
            ) - directAngle);
            double guessFactor = Math.max(-1, Math.min(1, offsetAngle / maxEscapeAngle)) * direction;
            int index = (int) Math.round(((guessFactor + 1) / 2) * (BINS - 1));
            return Math.max(0, Math.min(BINS - 1, index));
        }
    }

    private final java.util.List<EnemyWave> enemyWaves = new ArrayList<>();
    private double previousEnemyEnergy = 100;
    private String targetName = null;
    private double targetDistance = Double.MAX_VALUE;
    private int lateralDirection = 1;

    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setBodyColor(Color.BLACK);
        setGunColor(Color.WHITE);
        setRadarColor(Color.RED);

        setTurnRight(90);
        setAhead(100);

        while (true) {
            setTurnRadarRight(360);
            surfWaves();
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double energyDrop = previousEnemyEnergy - e.getEnergy();
        double absBearing = getHeadingRadians() + e.getBearingRadians();
        double lateralVelocity = getVelocity() * Math.sin(e.getBearingRadians());
        lateralDirection = (lateralVelocity >= 0) ? 1 : -1;

        if (energyDrop > 0 && energyDrop <= 3) {
            double bulletVelocity = 20 - 3 * energyDrop;
            Point2D.Double myLocation = new Point2D.Double(getX(), getY());
            Point2D.Double enemyLocation = project(myLocation, absBearing, e.getDistance());
            int direction = lateralDirection; // corrigido
            enemyWaves.add(new EnemyWave(enemyLocation, getTime(), bulletVelocity, absBearing, direction));
        }

        previousEnemyEnergy = e.getEnergy();

        if (targetName == null || e.getDistance() < targetDistance || !e.getName().equals(targetName)) {
            targetName = e.getName();
            targetDistance = e.getDistance();
        }

        if (!e.getName().equals(targetName)) return;

        double radarOffset = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
        setTurnRadarRightRadians(radarOffset * 2);

        double gunTurn = Utils.normalRelativeAngle(absBearing - getGunHeadingRadians());
        setTurnGunRightRadians(gunTurn);

        if (e.getDistance() < 150) {
            setFire(3);
        } else if (e.getDistance() < 300) {
            setFire(2);
        } else {
            setFire(1);
        }
    }

    private void surfWaves() {
        Point2D.Double myLocation = new Point2D.Double(getX(), getY());
        EnemyWave closestWave = null;
        double closestDistance = Double.MAX_VALUE;

        for (EnemyWave wave : enemyWaves) {
            wave.update(getTime());
            double distance = wave.fireLocation.distance(myLocation) - wave.distanceTraveled;
            if (distance < closestDistance && wave.distanceTraveled > 0) {
                closestDistance = distance;
                closestWave = wave;
            }
        }

        if (closestWave != null) {
            double dangerLeft = checkDanger(closestWave, -1, myLocation);
            double dangerRight = checkDanger(closestWave, 1, myLocation);

            int goDirection = (dangerLeft < dangerRight) ? -1 : 1;
            double angleOffset = closestWave.maxEscapeAngle * goDirection;
            double moveAngle = wallSmoothing(myLocation, closestWave.directAngle + angleOffset, goDirection == 1);
            setTurnRightRadians(Utils.normalRelativeAngle(moveAngle - getHeadingRadians()));
            setAhead(100);

            enemyWaves.removeIf(wave -> wave.distanceTraveled > wave.fireLocation.distance(myLocation) + 50);
        }
    }

    private double checkDanger(EnemyWave wave, int direction, Point2D.Double myLocation) {
        double guessFactor = direction * 1.0;
        double angleOffset = wave.maxEscapeAngle * guessFactor;
        double testAngle = wave.directAngle + angleOffset;
        Point2D.Double projected = project(myLocation, testAngle, wave.bulletVelocity);
        int index = wave.getFactorIndex(projected);
        return wave.guessFactors[index];
    }

    private double wallSmoothing(Point2D.Double location, double angle, boolean clockwise) {
        double wallStick = 30;
        double smoothedAngle = angle;
        Rectangle2D.Double fieldRect = new Rectangle2D.Double(18, 18,
                getBattleFieldWidth() - 36, getBattleFieldHeight() - 36);

        for (int i = 0; i < 100; i++) {
            Point2D.Double projected = project(location, smoothedAngle, wallStick);
            if (fieldRect.contains(projected)) break;
            smoothedAngle += (clockwise ? -0.05 : 0.05);
        }

        return smoothedAngle;
    }

    private Point2D.Double project(Point2D.Double source, double angle, double length) {
        return new Point2D.Double(source.x + Math.sin(angle) * length,
                                  source.y + Math.cos(angle) * length);
    }

    private double normalizeBearing(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private double normalRelativeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    private double absoluteBearing(double x1, double y1, double x2, double y2) {
        double xo = x2 - x1;
        double yo = y2 - y1;
        double hyp = Point2D.distance(x1, y1, x2, y2);
        double arcSin = Math.toDegrees(Math.asin(xo / hyp));
        double bearing = 0;

        if (xo > 0 && yo > 0) {
            bearing = arcSin;
        } else if (xo < 0 && yo > 0) {
            bearing = 360 + arcSin;
        } else if (xo > 0 && yo < 0) {
            bearing = 180 - arcSin;
        } else if (xo < 0 && yo < 0) {
            bearing = 180 - arcSin;
        }
        return bearing;
    }

    public void onHitByBullet(HitByBulletEvent e) {
        Point2D.Double myLocation = new Point2D.Double(getX(), getY());
        for (Iterator<EnemyWave> it = enemyWaves.iterator(); it.hasNext(); ) {
            EnemyWave wave = it.next();
            double distance = wave.fireLocation.distance(myLocation) - wave.distanceTraveled;
            if (Math.abs(distance) < wave.bulletVelocity) {
                int index = wave.getFactorIndex(myLocation);
                wave.guessFactors[index] += 1;
                it.remove();
                break;
            }
        }
    }

    public void onHitWall(HitWallEvent e) {
        // comportamento ao bater na parede (opcional)
    }

    public void onHitRobot(HitRobotEvent e) {
        setBack(100);
    }
}