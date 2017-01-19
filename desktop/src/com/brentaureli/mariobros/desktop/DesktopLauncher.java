package com.brentaureli.mariobros.desktop;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.brentaureli.mariobros.MarioBros;
import com.twilio.twiml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class DesktopLauncher {

	private static final Logger log = LoggerFactory.getLogger(DesktopLauncher.class);

	private final static String MARIO_MP3_5S = "http://themushroomkingdom.net/sounds/wav/smb/smb_stage_clear.wav";

	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1200;
		config.height = 624;
		MarioBros game = new MarioBros();
		new LwjglApplication(game, config);

		Set<String> calls = new HashSet<>();

		Spark.post("/mario", (req, res) -> {
			try {
				String from = req.queryParams("From");
				if (!calls.contains(from)) {
					calls.add(from);
					log.info("Init call from {}", from);

					res.type("application/xml");
					TwiML twiml = new VoiceResponse.Builder()
							.play(new Play.Builder(MARIO_MP3_5S).build())
							.say(new Say.Builder("Use phone keypad to control Mario and save the princess").build())
							.gather(new Gather.Builder().numDigits(1).timeout(3600).build())
							.build();
					return twiml.toXml();
				}

				String queryParam = req.queryParams("Digits");
				log.debug("From {} - Digit: {}", from, queryParam);

				Optional.ofNullable(queryParam).ifPresent(digit -> {
					switch (digit) {
						case "2":
							game.sendKey(Input.Keys.UP);
							break;
						case "4":
							game.sendKey(Input.Keys.LEFT);
							break;
						case "6":
							game.sendKey(Input.Keys.RIGHT);
							break;
						case "8":
							game.sendKey(Input.Keys.SPACE);
							break;
						default:
							log.info("Digit not supported: {}", digit);
					}
				});

				res.type("application/xml");
				TwiML twiml = new VoiceResponse.Builder().gather(
						new Gather.Builder().numDigits(1).timeout(3600).build()
				).build();
				return twiml.toXml();

			} catch (Exception e) {
				log.error("Error in POST to /mario", e);
				Spark.halt(503, "Sorry, service unavailable");
			}
			return null;
		});
	}
}
