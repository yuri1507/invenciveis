package invenciveis;

import robocode.*;
import java.awt.Color;

// O Blindado funciona como uma torre de defesa: quase não sai do lugar
// e concentra o dano nos tiros. Estende TeamRobot (herança).
public class Blindado extends TeamRobot {

	// Método principal: roda assim que a batalha começa
	public void run() {
		// Visual do robô (tons de laranja e amarelo)
		setBodyColor(Color.orange);
		setGunColor(Color.yellow);
		setRadarColor(Color.yellow);
		setScanColor(Color.orange);
		setBulletColor(Color.yellow);

		// Fica girando a arma sem parar, esperando algum inimigo cruzar o radar
		while (true) {
			turnGunRight(360);
		}
	}

	// Chamado automaticamente quando o radar encontra outro robô
	public void onScannedRobot(ScannedRobotEvent e) {
		// Parceiro de time não é alvo
		if (isTeammate(e.getName())) {
			return;
		}
		// Quanto mais perto o inimigo, mais forte o tiro:
		// de perto vale a pena gastar energia, de longe o tiro forte erra mais
		if (e.getDistance() < 300) {
			fire(3);
		} else {
			fire(1);
		}
	}

	// Chamado quando levamos um tiro
	public void onHitByBullet(HitByBulletEvent e) {
		turnRight(90); // sai da mira do inimigo
		ahead(100);    // andando para o lado
	}

	// Chamado quando batemos em uma parede
	public void onHitWall(HitWallEvent e) {
		back(50); // recua um pouco
	}
}
