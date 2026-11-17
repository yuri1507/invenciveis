package invenciveis;

import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.MessageEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

import java.awt.Color;

public class Perseguidor extends TeamRobot {

    private String alvo = null;
    private double alvoX = 0;
    private double alvoY = 0;
    private boolean temPosicao = false;
    private long ultimoScan = -100;
    private int zigue = 1;

    @Override
    public void run() {
        setColors(Color.RED, Color.ORANGE, Color.YELLOW);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            if (getTime() - ultimoScan > 2) {
                setTurnRadarRight(360);
                if (temPosicao) {
                    irPara(alvoX, alvoY);
                } else {
                    setTurnRight(30);
                    setAhead(80);
                }
            }
            if (getTime() % 15 == 0) {
                zigue = -zigue;
            }
            execute();
        }
    }

    private void irPara(double x, double y) {
        double angulo = Math.toDegrees(Math.atan2(x - getX(), y - getY()));
        setTurnRight(Utils.normalRelativeAngleDegrees(angulo - getHeading()));
        setAhead(100);
    }

    @Override
    public void onMessageReceived(MessageEvent e) {
        Object conteudo = e.getMessage();
        if (!(conteudo instanceof String)) {
            return;
        }
        String[] partes = ((String) conteudo).split(";");
        if (partes.length == 4 && partes[0].equals("ALVO")) {
            if (alvo == null || getTime() - ultimoScan > 10) {
                alvo = partes[1];
            }
            if (partes[1].equals(alvo)) {
                alvoX = Double.parseDouble(partes[2]);
                alvoY = Double.parseDouble(partes[3]);
                temPosicao = true;
            }
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        if (isTeammate(e.getName())) {
            return;
        }

        if (alvo != null && !e.getName().equals(alvo)
                && getTime() - ultimoScan <= 10 && e.getDistance() > 150) {
            return;
        }
        alvo = e.getName();
        ultimoScan = getTime();

        double distancia = e.getDistance();
        double anguloAbsoluto = getHeadingRadians() + e.getBearingRadians();

        alvoX = getX() + distancia * Math.sin(anguloAbsoluto);
        alvoY = getY() + distancia * Math.cos(anguloAbsoluto);
        temPosicao = true;

        double giroRadar = Utils.normalRelativeAngle(anguloAbsoluto - getRadarHeadingRadians());
        setTurnRadarRightRadians(giroRadar * 2);

        setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 20 * zigue));
        setAhead(distancia - 80);

        double potencia = (distancia < 200) ? 3 : 2;
        potencia = Math.min(potencia, Math.max(0.1, getEnergy() / 4));

        double giroArma = Utils.normalRelativeAngle(anguloAbsoluto - getGunHeadingRadians());
        setTurnGunRightRadians(giroArma);

        if (Math.abs(Math.toDegrees(giroArma)) < 15 && getGunHeat() == 0) {
            setFire(potencia);
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        setBack(80);
        setTurnRight(90);
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        if (isTeammate(e.getName())) {
            setBack(60);
            setTurnRight(45);
        } else {
            double giroArma = Utils.normalRelativeAngleDegrees(
                    getHeading() + e.getBearing() - getGunHeading());
            setTurnGunRight(giroArma);
            setFire(3);
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(alvo)) {
            alvo = null;
            temPosicao = false;
        }
    }
}
