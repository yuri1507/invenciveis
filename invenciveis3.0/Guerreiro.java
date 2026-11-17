package invenciveis;

import robocode.*;
import java.awt.Color;
import java.io.IOException;

// O Guerreiro é o robô que não para: circula pela arena, atira em quem
// aparece e avisa o parceiro (Blindado) quem é o inimigo encontrado.
public class Guerreiro extends TeamRobot {

	// Roda assim que a batalha começa
	public void run() {
		// Visual do robô (tons de cinza e branco)
		setBodyColor(Color.lightGray);
		setGunColor(Color.gray);
		setRadarColor(Color.white);
		setScanColor(Color.lightGray);
		setBulletColor(Color.white);

		// Repete esse movimento até o robô ser destruído
		while (true) {
			ahead(100);          // avança
			turnGunRight(360);   // gira a arma procurando inimigos
			turnRight(45);       // faz uma curva para a direita
			back(100);           // recua
			turnGunRight(360);   // gira a arma de novo
			turnLeft(45);        // desfaz a curva
		}
	}

	// Chamado quando o radar encontra outro robô
	public void onScannedRobot(ScannedRobotEvent e) {
		// Parceiro de time não é alvo
		if (isTeammate(e.getName())) {
			return;
		}
		fire(1); // tiro fraco, gasta pouca energia

		// Manda o nome do inimigo para o parceiro.
		// O try/catch é obrigatório porque o envio pode dar erro.
		try {
			broadcastMessage(e.getName());
		} catch (IOException erro) {
			// se não conseguir enviar, segue a batalha normalmente
		}
	}

	// Chamado quando levamos um tiro
	public void onHitByBullet(HitByBulletEvent e) {
		turnRight(90); // desvia para o lado
		ahead(100);    // e se afasta do atirador
	}

	// Chamado quando batemos em uma parede
	public void onHitWall(HitWallEvent e) {
		back(50);       // dá um passo para trás
		turnRight(90);  // e vira para não bater de novo
	}
}
