package invenciveis;

import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.IOException;

public class Atirador extends TeamRobot {

    private String alvo = null;
    private int direcao = 1;
    private long ultimoScan = -100;

    @Override
    public void run() {
        setColors(Color.BLUE, Color.CYAN, Color.WHITE);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            if (getTime() - ultimoScan > 10) {
                alvo = null;
            }
            if (getTime() - ultimoScan > 2) {
                setTurnRadarRight(Double.POSITIVE_INFINITY);
            }
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        if (isTeammate(e.getName())) {
            return;
        }

        if (alvo == null) {
            alvo = e.getName();
        }
        if (!e.getName().equals(alvo)) {
            return;
        }
        ultimoScan = getTime();

        double distancia = e.getDistance();
        double anguloAbsoluto = getHeadingRadians() + e.getBearingRadians();

        double inimigoX = getX() + distancia * Math.sin(anguloAbsoluto);
        double inimigoY = getY() + distancia * Math.cos(anguloAbsoluto);

        double giroRadar = Utils.normalRelativeAngle(anguloAbsoluto - getRadarHeadingRadians());
        setTurnRadarRightRadians(giroRadar * 2);

        double ajuste = (distancia > 300) ? 25 : -15;
        setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 90 - ajuste * direcao));
        setAhead(100 * direcao);
        if (Math.random() < 0.04) {
            direcao = -direcao;
        }

        double potencia;
        if (distancia < 150) {
            potencia = 3;
        } else if (distancia < 400) {
            potencia = 2;
        } else {
            potencia = 1;
        }
        potencia = Math.min(potencia, Math.max(0.1, getEnergy() / 4));

        double velocidadeBala = 20 - 3 * potencia;
        double velX = e.getVelocity() * Math.sin(e.getHeadingRadians());
        double velY = e.getVelocity() * Math.cos(e.getHeadingRadians());
        double previstoX = inimigoX;
        double previstoY = inimigoY;
        double tempo = 0;
        while ((++tempo) * velocidadeBala < Point2D.distance(getX(), getY(), previstoX, previstoY)) {
            previstoX += velX;
            previstoY += velY;
            previstoX = Math.max(18, Math.min(getBattleFieldWidth() - 18, previstoX));
            previstoY = Math.max(18, Math.min(getBattleFieldHeight() - 18, previstoY));
            if (tempo > 200) {
                break;
            }
        }
        double anguloTiro = Math.atan2(previstoX - getX(), previstoY - getY());
        double giroArma = Utils.normalRelativeAngle(anguloTiro - getGunHeadingRadians());
        setTurnGunRightRadians(giroArma);

        if (Math.abs(Math.toDegrees(giroArma)) < 10 && getGunHeat() == 0) {
            setFire(potencia);
        }

        try {
            broadcastMessage("ALVO;" + e.getName() + ";" + inimigoX + ";" + inimigoY);
        } catch (IOException ex) {
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        direcao = -direcao;
        setAhead(100 * direcao);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        direcao = -direcao;
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        direcao = -direcao;
        setBack(60);
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(alvo)) {
            alvo = null;
        }
    }
}
