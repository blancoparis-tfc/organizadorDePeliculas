package org.dbp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import static java.util.stream.Collectors.*;

import java.util.stream.Stream;

import org.dbp.util.CapitulosEnDirectorios;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ficheros {
	
	private static Logger logger = LoggerFactory.getLogger(Ficheros.class);
	
	public static void main(String[] args) throws IOException {
		logger.debug("INICIO");
		//kong\t1
		//Erase una vez Z:\series\wairnes pines\T2
		CapitulosEnDirectorios.istancia(Paths.get("Z:\\series")).procesarFicheros(false,false);
		logger.debug("FIN");
	}

	
	
}
