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

        public EnemyWave(Point2D.Double fireLocation, long fireTime, double bulletVelocity, double directAngle) {
            this.fireLocation = fireLocation;
            this.fireTime = fireTime;
            this.bulletVelocity = bulletVelocity;
            this.directAngle = directAngle;
        }

        public void update(long time) {
            distanceTraveled = (time - fireTime) * bulletVelocity;
        }
    }

    private final java.util.List<EnemyWave> enemyWaves = new ArrayList<>();
    private double previousEnemyEnergy = 100;

    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setBodyColor(Color.BLACK);
        setGunColor(Color.WHITE);
        setRadarColor(Color.RED);

        while (true) {
            setTurnRadarRight(360); // Radar sempre girando
            surfWaves();
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        // Detecta tiros (queda de energia do inimigo)
        double energyDrop = previousEnemyEnergy - e.getEnergy();
        if (energyDrop > 0 && energyDrop <= 3) {
            double bulletVelocity = 20 - 3 * energyDrop;
            double absBearing = Math.toRadians(getHeading() + e.getBearing());
            Point2D.Double myLocation = new Point2D.Double(getX(), getY());
            Point2D.Double enemyLocation = project(myLocation, absBearing, e.getDistance());

            enemyWaves.add(new EnemyWave(enemyLocation, getTime(), bulletVelocity, absBearing));
        }

        previousEnemyEnergy = e.getEnergy();

        // Mira simples (head-on ou leading shot básico)
        double firePower = Math.min(3, Math.max(1, 400 / e.getDistance()));
        double bulletSpeed = 20 - firePower * 3;
        long time = (long)(e.getDistance() / bulletSpeed);
        double absBearingDeg = getHeading() + e.getBearing();
        double x = getX() + Math.sin(Math.toRadians(absBearingDeg)) * e.getDistance();
        double y = getY() + Math.cos(Math.toRadians(absBearingDeg)) * e.getDistance();
        double futureX = x + Math.sin(Math.toRadians(getHeading())) * getVelocity() * time;
        double futureY = y + Math.cos(Math.toRadians(getHeading())) * getVelocity() * time;
        double absDeg = absoluteBearing(getX(), getY(), futureX, futureY);
		setTurnGunRight(getGunHeading()));
        //setTurnGunRight(normalizeBearing(absDeg - getGunHeading()));

        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
            setFire(firePower);
        }

        // Trava o radar no inimigo
        double radarTurn = Utils.normalRelativeAngleDegrees(getHeading() + e.getBearing() - getRadarHeading());
        setTurnRadarRight(radarTurn);
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
            double angleToWave = Math.atan2(
                myLocation.x - closestWave.fireLocation.x,
                myLocation.y - closestWave.fireLocation.y
            );

            double moveAngle = wallSmoothing(myLocation, angleToWave + Math.PI / 2, true);
            setTurnRightRadians(normalRelativeAngle(moveAngle - Math.toRadians(getHeading())));
            setAhead(100);
        }
    }

    private double wallSmoothing(Point2D.Double location, double angle, boolean clockwise) {
        double wallStick = 120;
        double smoothedAngle = angle;
        Rectangle2D.Double fieldRect = new Rectangle2D.Double(18, 18,
                getBattleFieldWidth() - 36, getBattleFieldHeight() - 36);

        for (int i = 0; i < 100; i++) {
            Point2D.Double projected = project(location, smoothedAngle, wallStick);
            if (fieldRect.contains(projected)) {
                break;
            }
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
        setBack(100);
        setTurnRight(90);
    }

    public void onHitWall(HitWallEvent e) {
        setBack(50);
        setTurnRight(90);
    }

    public void onHitRobot(HitRobotEvent e) {
        setBack(50);
        setTurnRight(90);
    }
}
