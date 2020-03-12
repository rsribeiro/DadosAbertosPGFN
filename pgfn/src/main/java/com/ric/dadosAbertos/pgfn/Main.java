package com.ric.dadosAbertos.pgfn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {
	public static void main(String[] args) {
		Options options = new Options()
				.addOption("acnpj", "arqcnpj", true, "(Opcional) Arquivo com a lista de CNPJs a serem filtrados")
				.addOption("dfgts", "dirfgts", true, "(Opcional) Diretório com os arquivos da dívida FGTS")
				.addOption("dnprev", "dirnprev", true, "(Opcional) Diretório com os arquivos do dívida não previdenciária")
				.addOption("dprev", "dirprev", true, "(Opcional) Diretório com os arquivos do dívida previdenciária")
				.addRequiredOption("o", "output", true, "(Obrigatório) Diretório de saída")
				;

		try {
			CommandLine cmd = parseOptions(args, options);

			run(cmd);
		} catch (RuntimeException e) {
			System.err.println("Erro: " + e.getLocalizedMessage());

			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("JuntaArquivosPGFN", options);
		}
	}

	private static void run(CommandLine cmd) {
		Path dirSaida = Paths.get(cmd.getOptionValue('o'));
		validaParametroDiretorioLeituraEscrita(dirSaida);

		Optional<Path> arqCNPJ = cmd.hasOption("acnpj") ? Optional.of(Paths.get(cmd.getOptionValue("acnpj"))) : Optional.empty();
		if (arqCNPJ.isPresent()) {
			validaParametroArquivo(arqCNPJ.get());
		}

		Path dirFGTS;
		Path dirNaoPrevidenciario;
		Path dirPrevidenciario;

		JuntaArquivosPGFN juntador = new JuntaArquivosPGFN();

		long t0 = System.nanoTime();
		try {
			if (cmd.hasOption("dfgts") || cmd.hasOption("dprev") || cmd.hasOption("dnprev")) {
				if (cmd.hasOption("dfgts")) {
					dirFGTS = Paths.get(cmd.getOptionValue("dfgts"));
					validaParametroDiretorioLeitura(dirFGTS);
				} else {
					throw new RuntimeException("Erro nos parâmetros.");
				}
				if (cmd.hasOption("dnprev")) {
					dirNaoPrevidenciario = Paths.get(cmd.getOptionValue("dnprev"));
					validaParametroDiretorioLeitura(dirNaoPrevidenciario);
				} else {
					throw new RuntimeException("Erro nos parâmetros.");
				}
				if (cmd.hasOption("dprev")) {
					dirPrevidenciario = Paths.get(cmd.getOptionValue("dprev"));
					validaParametroDiretorioLeitura(dirPrevidenciario);
				} else {
					throw new RuntimeException("Erro nos parâmetros.");
				}
			} else {
				BaixadorArquivos baixador = new BaixadorArquivos();

				dirFGTS = Files.createTempDirectory("pgfn_");
				dirNaoPrevidenciario = Files.createTempDirectory("pgfn_");
				dirPrevidenciario = Files.createTempDirectory("pgfn_");

				baixador.baixarArquivos(dirFGTS, dirNaoPrevidenciario, dirPrevidenciario);
			}

			juntador.juntaArquivos(arqCNPJ, dirFGTS, dirPrevidenciario, dirNaoPrevidenciario, dirSaida);
		} catch (IOException e) {
			throw new RuntimeException(e.getLocalizedMessage(), e);
		}

		double deltaT = (System.nanoTime() - t0)/1E9;
		System.out.println("Script executado em " + deltaT + "s.");
	}

	private static CommandLine parseOptions(String[] args, Options options) {
		CommandLineParser parser = new DefaultParser();
		try {
			return parser.parse(options, args);
		} catch (ParseException e) {
			throw new RuntimeException("Erro nos parâmetros.", e);
		}
	}

	private static void validaParametroArquivo(Path path) {
		if (!Files.exists(path) || Files.isDirectory(path) || !Files.isReadable(path)) {
			throw new RuntimeException("Certifique-se de que o arquivo '" + path + "' existe e há acesso de leitura.");
		}
	}

	private static void validaParametroDiretorioLeitura(Path path) {
		if (!Files.exists(path) || !Files.isDirectory(path) || !Files.isReadable(path)) {
			throw new RuntimeException("Certifique-se de que o diretório '" + path + "' existe e há acesso de leitura.");
		}
	}

	private static void validaParametroDiretorioLeituraEscrita(Path path) {
		if (!Files.exists(path) || !Files.isDirectory(path) || !Files.isReadable(path) || !Files.isWritable(path)) {
			throw new RuntimeException("Certifique-se de que o diretório '" + path + "' existe e há acesso de leitura e escrita.");
		}
	}
}
