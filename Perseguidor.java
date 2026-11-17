package invenciveis;

import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.MessageEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

import java.awt.Color;

/**
 * PERSEGUIDOR - Robô 2 do time "Invenciveis".
 *
 * Papel no time: AGRESSIVO / CORPO A CORPO.
 *  - Recebe do Atirador o alvo e a posição dele (onMessageReceived).
 *  - Vai até o inimigo em zigue-zague e atira com potência alta de perto.
 *  - Nunca ataca o parceiro (isTeammate).
 *
 * HERANÇA: Perseguidor -> TeamRobot -> AdvancedRobot -> Robot
 */
public class Perseguidor extends TeamRobot {

    // ---------- ATRIBUTOS (estado do robô) ----------
    private String alvo = null;          // nome do inimigo perseguido
    private double alvoX = 0;            // última posição conhecida do alvo
    private double alvoY = 0;
    private boolean temPosicao = false;  // já sabemos onde o alvo está?
    private long ultimoScan = -100;      // último turno em que vimos o alvo
    private int zigue = 1;               // alterna o zigue-zague (1 / -1)

    // ---------- LOOP PRINCIPAL ----------
    @Override
    public void run() {
        setColors(Color.RED, Color.ORANGE, Color.YELLOW);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            // Se não estamos vendo o alvo agora...
            if (getTime() - ultimoScan > 2) {
                setTurnRadarRight(360); // ...gira o radar procurando
                if (temPosicao) {
                    // ...e vai até a última posição conhecida (informada pelo parceiro)
                    irPara(alvoX, alvoY);
                } else {
                    // ...ou anda em círculo explorando a arena
                    setTurnRight(30);
                    setAhead(80);
                }
            }
            // Troca o lado do zigue-zague a cada 15 turnos
            if (getTime() % 15 == 0) {
                zigue = -zigue;
            }
            execute();
        }
    }

    // Método auxiliar: vira o corpo e anda até uma coordenada (x, y)
    private void irPara(double x, double y) {
        double angulo = Math.toDegrees(Math.atan2(x - getX(), y - getY()));
        setTurnRight(Utils.normalRelativeAngleDegrees(angulo - getHeading()));
        setAhead(100);
    }

    // ---------- EVENTO: MENSAGEM DO PARCEIRO ----------
    // Formato recebido: "ALVO;nome;x;y"
    @Override
    public void onMessageReceived(MessageEvent e) {
        Object conteudo = e.getMessage();
        if (!(conteudo instanceof String)) {
            return;
        }
        String[] partes = ((String) conteudo).split(";");
        if (partes.length == 4 && partes[0].equals("ALVO")) {
            // Só adota o alvo do parceiro se não estamos vendo ninguém no momento
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

    // ---------- EVENTO: RADAR VIU UM ROBÔ ----------
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Nunca ataca o parceiro!
        if (isTeammate(e.getName())) {
            return;
        }

        // Se já temos alvo, só muda se outro inimigo estiver MUITO perto (ameaça imediata)
        if (alvo != null && !e.getName().equals(alvo)
                && getTime() - ultimoScan <= 10 && e.getDistance() > 150) {
            return;
        }
        alvo = e.getName();
        ultimoScan = getTime();

        double distancia = e.getDistance();
        double anguloAbsoluto = getHeadingRadians() + e.getBearingRadians();

        // Atualiza a posição conhecida do alvo
        alvoX = getX() + distancia * Math.sin(anguloAbsoluto);
        alvoY = getY() + distancia * Math.cos(anguloAbsoluto);
        temPosicao = true;

        // 1) RADAR: trava no alvo
        double giroRadar = Utils.normalRelativeAngle(anguloAbsoluto - getRadarHeadingRadians());
        setTurnRadarRightRadians(giroRadar * 2);

        // 2) MOVIMENTO: avança direto no inimigo, mas com zigue-zague (+/- 20 graus)
        setTurnRight(Utils.normalRelativeAngleDegrees(e.getBearing() + 20 * zigue));
        setAhead(distancia - 80); // para a uma distância curta, sem ficar colado

        // 3) POTÊNCIA: perto = máxima (3), longe = 2
        double potencia = (distancia < 200) ? 3 : 2;
        potencia = Math.min(potencia, Math.max(0.1, getEnergy() / 4));

        // 4) MIRA: aponta direto para o inimigo (de perto não precisa de previsão)
        double giroArma = Utils.normalRelativeAngle(anguloAbsoluto - getGunHeadingRadians());
        setTurnGunRightRadians(giroArma);

        if (Math.abs(Math.toDegrees(giroArma)) < 15 && getGunHeat() == 0) {
            setFire(potencia);
        }
    }

    // ---------- OUTROS EVENTOS ----------

    // Bateu na parede: dá ré e vira para sair dela
    @Override
    public void onHitWall(HitWallEvent e) {
        setBack(80);
        setTurnRight(90);
    }

    // Esbarrou em alguém: se for inimigo, atira com força total; se for parceiro, recua
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

    // Se o alvo morreu, esquece ele e procura outro
    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(alvo)) {
            alvo = null;
            temPosicao = false;
        }
    }
}
