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
            this.maxEscapeAngle = Math.asin(10.0 / bulletVelocity);
        }

        public void update(long time) {
            distanceTraveled = (time - fireTime) * bulletVelocity;
        }
	public int getFactorIndex(Point2D.Double hitLocation) { // guess factor calcula a melhor posicao pra se mover baseado onde foi atingido por tiros
        double offsetAngle = Utils.normalRelativeAngle(Math.atan2(
            hitLocation.x - fireLocation.x,
            hitLocation.y - fireLocation.y
        ) - directAngle);
        double guessFactor = Math.max(-1, Math.min(1, offsetAngle / maxEscapeAngle)) * direction;
        return (int) Math.round(((guessFactor + 1) / 2) * (BINS - 1));
    	}
    }

    private final java.util.List<EnemyWave> enemyWaves = new ArrayList<>();
    private double previousEnemyEnergy = 100;
    private String targetName = null; // inimigo atual
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
            setTurnRadarRight(360); // radar sempre girando
            surfWaves(); // se mover
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double energyDrop = previousEnemyEnergy - e.getEnergy();
		double absBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyHeading = absBearing;
		double enemyBearing = e.getBearingRadians();
		double myHeading = getHeadingRadians();
		double lateralVelocity = getVelocity() * Math.sin(enemyBearing);
		lateralDirection = (lateralVelocity >= 0) ? 1 : -1;
        if (energyDrop > 0 && energyDrop <= 3) { // detecta tiros da queda de energia do inimigo
            double bulletVelocity = 20 - 3 * energyDrop;
            absBearing = getHeadingRadians() + e.getBearingRadians();
            Point2D.Double myLocation = new Point2D.Double(getX(), getY());
            Point2D.Double enemyLocation = project(myLocation, absBearing, e.getDistance());
	    int direction = 1;
            enemyWaves.add(new EnemyWave(enemyLocation, getTime(), bulletVelocity, absBearing, direction));
        }

        previousEnemyEnergy = e.getEnergy();
	if (targetName == null || e.getDistance() < targetDistance || !e.getName().equals(targetName)) { // troca de alvo se outro robo inimigo estiver mais perto
            targetName = e.getName();
            targetDistance = e.getDistance();
        }
	if (!e.getName().equals(targetName)) return; // so interage com esse robo
	double radarOffset = Utils.normalRelativeAngle(getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians());
		setTurnRadarRightRadians(radarOffset * 2); // trava o radar no inimigo
	double gunTurn = getHeadingRadians() + e.getBearingRadians() - getGunHeadingRadians();
        setTurnGunRightRadians(normalizeBearing(gunTurn)); // trava a arma no robo inimigo
        if (e.getDistance() < 150) { // atira com diferentes potencias se estiver a uma certa distancia 
            setFire(3);
        } else if (e.getDistance() < 300) {
            setFire(2);
        } else {
            setFire(1);
        }
    }

    private void surfWaves() { // o robo se move baseado nas possiveis posicoes do tiro de outro robo
        Point2D.Double myLocation = new Point2D.Double(getX(), getY());
    EnemyWave closestWave = null;
    double closestDistance = Double.MAX_VALUE;

    // encontra a onda mais próxima
    for (EnemyWave wave : enemyWaves) {
        wave.update(getTime());
        double distance = wave.fireLocation.distance(myLocation) - wave.distanceTraveled;
        if (distance < closestDistance && wave.distanceTraveled > 0) {
            closestDistance = distance;
            closestWave = wave;
        }
    }

    if (closestWave != null) {
        // simula o perigo de mover para a esquerda ou para a direita
        double dangerLeft = checkDanger(closestWave, -1, myLocation);
        double dangerRight = checkDanger(closestWave, 1, myLocation);

        // escolhe a direção mais segura
        int goDirection = (dangerLeft < dangerRight) ? -1 : 1;

        double angleOffset = closestWave.maxEscapeAngle * goDirection;
        double moveAngle = wallSmoothing(myLocation, closestWave.directAngle + angleOffset, goDirection == 1);
        setTurnRightRadians(normalRelativeAngle(moveAngle - getHeadingRadians()));
        setAhead(100);

        // remove ondas antigas
        enemyWaves.removeIf(wave ->
            wave.distanceTraveled > wave.fireLocation.distance(myLocation) + 50);
    }
}

    private double wallSmoothing(Point2D.Double location, double angle, boolean clockwise) { // para nao bater na parede
        double wallStick = 30;
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
	private double checkDanger(EnemyWave wave, int direction, Point2D.Double myLocation) {
    double guessFactor = direction * 1.0;
    double angleOffset = wave.maxEscapeAngle * guessFactor;
    double testAngle = wave.directAngle + angleOffset;
    Point2D.Double projected = project(myLocation, testAngle, wave.bulletVelocity);
    int index = wave.getFactorIndex(projected);
    return wave.guessFactors[index];
}

    public void onHitByBullet(HitByBulletEvent e) {
		Point2D.Double myLocation = new Point2D.Double(getX(), getY());
    	for (Iterator<EnemyWave> it = enemyWaves.iterator(); it.hasNext(); ) {
        	EnemyWave wave = it.next();
        	double distance = wave.fireLocation.distance(myLocation) - wave.distanceTraveled;
        	if (Math.abs(distance) < wave.bulletVelocity) {
            	int index = wave.getFactorIndex(myLocation);
            	wave.guessFactors[index] += 1;
            	it.remove(); // remove a onda que acertou
            	break;
        	}
    	}
    }

    public void onHitWall(HitWallEvent e) {

    }

    public void onHitRobot(HitRobotEvent e) {
        setBack(100);
    }
}
