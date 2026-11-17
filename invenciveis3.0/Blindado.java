package invenciveis;

import robocode.*;
import java.awt.Color;

// O Blindado funciona como uma torre de defesa: quase não sai do lugar
// e dá prioridade ao inimigo que o Guerreiro avisou.
public class Blindado extends TeamRobot {

	// Nome do inimigo que o parceiro avisou (null = ainda não recebeu aviso)
	private String alvo = null;

	// Roda assim que a batalha começa
	public void run() {
		// Visual do robô (tons de laranja e amarelo)
		setBodyColor(Color.orange);
		setGunColor(Color.yellow);
		setRadarColor(Color.yellow);
		setScanColor(Color.orange);
		setBulletColor(Color.yellow);

		// Fica girando a arma, esperando algum inimigo cruzar o radar
		while (true) {
			turnGunRight(360);
		}
	}

	// Chamado quando o parceiro manda uma mensagem
	public void onMessageReceived(MessageEvent e) {
		alvo = (String) e.getMessage(); // a mensagem é o nome do inimigo
	}

	// Chamado quando o radar encontra outro robô
	public void onScannedRobot(ScannedRobotEvent e) {
		// Parceiro de time não é alvo
		if (isTeammate(e.getName())) {
			return;
		}
		// Se for o inimigo avisado pelo parceiro, atira com força total.
		// Senão, a força depende da distância: de perto tiro forte, de longe fraco.
		if (e.getName().equals(alvo)) {
			fire(3);
		} else if (e.getDistance() < 300) {
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
