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

/**
 * ATIRADOR - Robô 1 do time "Invenciveis".
 *
 * Papel no time: SNIPER / SUPORTE DE LONGA DISTÂNCIA.
 *  - Mantém distância do inimigo e anda em círculos (strafing) para ser difícil de acertar.
 *  - Trava o radar no alvo e mira com PREVISÃO DE MOVIMENTO (tiro preditivo).
 *  - Avisa o parceiro (Perseguidor) quem é o alvo e onde ele está.
 *
 * HERANÇA: Atirador -> TeamRobot -> AdvancedRobot -> Robot
 *  - de AdvancedRobot herdamos comandos não bloqueantes (setAhead, setFire, execute...)
 *  - de TeamRobot herdamos a comunicação (broadcastMessage, isTeammate...)
 */
public class Atirador extends TeamRobot {

    // ---------- ATRIBUTOS (estado do robô) ----------
    private String alvo = null;        // nome do inimigo que estamos focando
    private int direcao = 1;           // 1 = sentido horário, -1 = anti-horário
    private long ultimoScan = -100;    // turno em que vimos o alvo pela última vez

    // ---------- LOOP PRINCIPAL ----------
    @Override
    public void run() {
        // Cores do robô (identidade visual do time)
        setColors(Color.BLUE, Color.CYAN, Color.WHITE);

        // Deixa arma e radar girarem de forma independente do corpo
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            // Se perdemos o alvo por mais de 10 turnos, esquece ele e procura outro
            if (getTime() - ultimoScan > 10) {
                alvo = null;
            }
            // Se não está travado em ninguém, gira o radar sem parar procurando
            if (getTime() - ultimoScan > 2) {
                setTurnRadarRight(Double.POSITIVE_INFINITY);
            }
            // execute() faz TODAS as ações agendadas acontecerem juntas neste turno
            execute();
        }
    }

    // ---------- EVENTO: RADAR VIU UM ROBÔ ----------
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Nunca atira no parceiro!
        if (isTeammate(e.getName())) {
            return;
        }

        // Escolhe o alvo: o primeiro inimigo visto (ou o atual, se já temos um)
        if (alvo == null) {
            alvo = e.getName();
        }
        if (!e.getName().equals(alvo)) {
            return; // ignora outros inimigos enquanto focamos no alvo
        }
        ultimoScan = getTime();

        double distancia = e.getDistance();
        // Ângulo absoluto (em relação ao norte da arena) até o inimigo, em radianos
        double anguloAbsoluto = getHeadingRadians() + e.getBearingRadians();

        // Posição atual do inimigo
        double inimigoX = getX() + distancia * Math.sin(anguloAbsoluto);
        double inimigoY = getY() + distancia * Math.cos(anguloAbsoluto);

        // 1) RADAR: trava no alvo (gira o dobro do necessário para não perder)
        double giroRadar = Utils.normalRelativeAngle(anguloAbsoluto - getRadarHeadingRadians());
        setTurnRadarRightRadians(giroRadar * 2);

        // 2) MOVIMENTO: anda de lado (perpendicular ao inimigo) formando um círculo.
        //    Se está longe aproxima um pouco; se está perto, se afasta um pouco.
        double ajuste = (distancia > 300) ? 25 : -15;
        setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 90 - ajuste * direcao));
        setAhead(100 * direcao);
        // De vez em quando inverte o sentido para ficar imprevisível
        if (Math.random() < 0.04) {
            direcao = -direcao;
        }

        // 3) POTÊNCIA DO TIRO: perto = forte (3), médio = 2, longe = fraco (1, mais rápido)
        double potencia;
        if (distancia < 150) {
            potencia = 3;
        } else if (distancia < 400) {
            potencia = 2;
        } else {
            potencia = 1;
        }
        // Não gasta energia que não tem
        potencia = Math.min(potencia, Math.max(0.1, getEnergy() / 4));

        // 4) MIRA PREDITIVA: calcula onde o inimigo estará quando a bala chegar
        double velocidadeBala = 20 - 3 * potencia;
        double velX = e.getVelocity() * Math.sin(e.getHeadingRadians());
        double velY = e.getVelocity() * Math.cos(e.getHeadingRadians());
        double previstoX = inimigoX;
        double previstoY = inimigoY;
        double tempo = 0;
        while ((++tempo) * velocidadeBala < Point2D.distance(getX(), getY(), previstoX, previstoY)) {
            previstoX += velX;
            previstoY += velY;
            // Não deixa a previsão sair da arena
            previstoX = Math.max(18, Math.min(getBattleFieldWidth() - 18, previstoX));
            previstoY = Math.max(18, Math.min(getBattleFieldHeight() - 18, previstoY));
            if (tempo > 200) {
                break; // segurança contra loop longo
            }
        }
        double anguloTiro = Math.atan2(previstoX - getX(), previstoY - getY());
        double giroArma = Utils.normalRelativeAngle(anguloTiro - getGunHeadingRadians());
        setTurnGunRightRadians(giroArma);

        // Atira só se a arma já está bem apontada e não está superaquecida
        if (Math.abs(Math.toDegrees(giroArma)) < 10 && getGunHeat() == 0) {
            setFire(potencia);
        }

        // 5) COMUNICAÇÃO: avisa o parceiro quem é o alvo e onde ele está
        try {
            broadcastMessage("ALVO;" + e.getName() + ";" + inimigoX + ";" + inimigoY);
        } catch (IOException ex) {
            // se falhar o envio, o robô simplesmente continua lutando
        }
    }

    // ---------- OUTROS EVENTOS ----------

    // Bateu na parede: inverte o sentido do movimento
    @Override
    public void onHitWall(HitWallEvent e) {
        direcao = -direcao;
        setAhead(100 * direcao);
    }

    // Levou um tiro: muda de direção para dificultar os próximos
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        direcao = -direcao;
    }

    // Esbarrou em alguém: se afasta (principalmente se for o parceiro)
    @Override
    public void onHitRobot(HitRobotEvent e) {
        direcao = -direcao;
        setBack(60);
    }

    // Se o alvo morreu, procura um novo
    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(alvo)) {
            alvo = null;
        }
    }
}
