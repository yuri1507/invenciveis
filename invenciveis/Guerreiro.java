package invenciveis;

import robocode.*;
import java.awt.Color;

// O Guerreiro é o robô que não para: fica circulando pela arena e
// atirando em quem aparecer na frente. Estende TeamRobot (herança).
public class Guerreiro extends TeamRobot {

	// Método principal: roda assim que a batalha começa
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
			turnLeft(45);        // volta a curva para a esquerda
		}
	}

	// Chamado automaticamente quando o radar encontra outro robô
	public void onScannedRobot(ScannedRobotEvent e) {
		// Se quem apareceu é do nosso time, não faz nada
		if (isTeammate(e.getName())) {
			return;
		}
		fire(1); // dispara um tiro fraco, gasta pouca energia
	}

	// Chamado quando levamos um tiro
	public void onHitByBullet(HitByBulletEvent e) {
		turnRight(90); // desvia para o lado
		ahead(100);    // e sai andando para longe do atirador
	}

	// Chamado quando batemos em uma parede
	public void onHitWall(HitWallEvent e) {
		back(50);       // dá um passo para trás
		turnRight(90);  // e vira para não bater de novo
	}
}
