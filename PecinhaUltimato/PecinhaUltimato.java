package PecinhaUltimato;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;

public class PecinhaUltimato extends AdvancedRobot {
    private boolean movingForward = true;

    public void run() {
        setBodyColor(Color.black);
        setRadarColor(Color.white);
        setGunColor(Color.black);
        setBulletColor(Color.red);

        // Permitir movimentos independentes
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        // Loop de varredura com o radar
        while (true) {
            setTurnRadarRight(360);
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        // Cálculo do ângulo absoluto do inimigo (em radianos)
        double absoluteBearing = Math.toRadians(getHeading()) + Math.toRadians(e.getBearing());

        // Ângulo relativo entre o canhão e o inimigo
        double bearingFromGun = Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians());
        double bearingFromRadar = Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians());

        // Apontar canhão e radar
        setTurnGunRightRadians(bearingFromGun);
        setTurnRadarRightRadians(bearingFromRadar);

        // Atirar se estiver alinhado e em alcance
        if (Math.abs(bearingFromGun) < Math.toRadians(10) && getGunHeat() == 0 && e.getDistance() < 500) {
            setFire(3);
        }

        // Evitar colisão quando muito perto
        if (e.getDistance() < 100) {
            reverseDirection();
        }

        setAhead(150);
    }

    public void onHitWall(HitWallEvent e) {
        reverseDirection();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        reverseDirection();
    }

    private void reverseDirection() {
        if (movingForward) {
            setBack(150);
            movingForward = false;
        } else {
            setAhead(150);
            movingForward = true;
        }
    }
}