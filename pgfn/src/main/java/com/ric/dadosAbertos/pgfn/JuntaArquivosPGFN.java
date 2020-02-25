/*
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.ric.dadosAbertos.pgfn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Este programa lê os arquivos disponibilizados no sítio eletrônico da Procuradoria
 * Geral da Fazenda Nacional (PGFN), em https://www.pgfn.fazenda.gov.br/acesso-a-informacao/dados-abertos
 * juntando em apenas um arquivo as entradas referentes aos CNPJs de interesse.
 *
 * Parâmetros:
 * Opcional: Arquivo texto contendo os CNPJs de interesse, um em cada linha
 * Obrigatório: Diretório com a base da dívida FGTS
 * Obrigatório: Diretório com a base da dívida previdenciária
 * Obrigatório: Diretório com a base da dívida geral
 * Obrigatório: Diretório de saída
 *
 * @author Ricardo Ribeiro
 */
public class JuntaArquivosPGFN {
	private static final Charset CHARSET = StandardCharsets.ISO_8859_1;

	public static void main(String[] args) throws IOException {
		if (args.length != 4 && args.length != 5) {
			System.err.println("ERRO! Parâmetros de entrada diferente do formato esperado.");
			System.err.println("Para simplesmente juntar todos os arquivos em um só, o formato esperado é:");
			System.err.println("'<diretório FGTS> <diretório previdenciário> <diretório não previdenciário> <diretório saída>'");
			System.err.println("Para filtrar o resultado final segundo uma lista de CNPJs, o formato esperado é:");
			System.err.println("'<arquivo CNPJ> <diretório FGTS> <diretório previdenciário> <diretório não previdenciário> <diretório saída>'");
			System.exit(1);
		}

		Optional<Path> arqCNPJ;
		Path dirFGTS;
		Path dirPrevidenciario;
		Path dirNaoPrevidenciario;
		Path dirSaida;

		if (args.length == 4) {
			arqCNPJ = Optional.empty();
			dirFGTS = Paths.get(args[0]);
			dirPrevidenciario = Paths.get(args[1]);
			dirNaoPrevidenciario = Paths.get(args[2]);
			dirSaida = Paths.get(args[3]);
		} else {
			arqCNPJ = Optional.of(Paths.get(args[0]));
			dirFGTS = Paths.get(args[1]);
			dirPrevidenciario = Paths.get(args[2]);
			dirNaoPrevidenciario = Paths.get(args[3]);
			dirSaida = Paths.get(args[4]);
		}

		if (arqCNPJ.isPresent()) {
			validaParametroArquivo(arqCNPJ.get());
		}
		validaParametroDiretorioLeitura(dirFGTS);
		validaParametroDiretorioLeitura(dirPrevidenciario);
		validaParametroDiretorioLeitura(dirNaoPrevidenciario);
		validaParametroDiretorioLeituraEscrita(dirSaida);

		System.out.println("Parâmetros:");
		if (arqCNPJ.isPresent()) {
			System.out.println("Arquivo CNPJ = " + arqCNPJ.get());
		}
		System.out.println("Diretório FGTS = " + dirFGTS);
		System.out.println("Diretório Previdenciário = " + dirPrevidenciario);
		System.out.println("Diretório Não Previdenciário = " + dirNaoPrevidenciario);
		System.out.println("Diretório Saída = " + dirSaida);

		long t0 = System.nanoTime();
		executaPrograma(arqCNPJ, dirFGTS, dirPrevidenciario, dirNaoPrevidenciario, dirSaida);
		double deltaT = (System.nanoTime() - t0)/1E9;
		System.out.println("Script executado em " + deltaT + "s.");
	}

	private static void executaPrograma(Optional<Path> arqCNPJ, Path dirFGTS, Path dirPrevidenciario, Path dirNaoPrevidenciario, Path dirSaida) throws IOException {
		Path arqFGTS = dirSaida.resolve("fgts.csv");
		Path arqPrevidenciario = dirSaida.resolve("previdenciario.csv");
		Path arqNaoPrevidenciario = dirSaida.resolve("nao_previdenciario.csv");
		Path arqSaida = dirSaida.resolve("base_consolidada.csv");

		Set<String> conjuntoCNPJs = leArquivoCNPJs(arqCNPJ);
		if (conjuntoCNPJs.isEmpty()) {
			System.out.println("Não foi passada conjunto de CNPJs a serem filtrados.");
		} else {
			System.out.println("Conjunto de CNPJs a serem filtrados tem tamanho = " + conjuntoCNPJs.size());
		}

		//Primeiro lê cada tipo de base e cria um arquivo de saída para cada base, contendo apenas os CNPJs filtrados
		criaBaseIndividual(arqFGTS, dirFGTS, "FGTS", conjuntoCNPJs);
		criaBaseIndividual(arqPrevidenciario, dirPrevidenciario, "PREVIDENCIARIO", conjuntoCNPJs);
		criaBaseIndividual(arqNaoPrevidenciario, dirNaoPrevidenciario, "NAO_PREVIDENCIARIO", conjuntoCNPJs);

		//Junta os arquivos de cada base em uma base consolidada, colocando vazio quando a coluna for inexistente
		criaBaseConsolidada(arqFGTS, arqPrevidenciario, arqNaoPrevidenciario, arqSaida);
	}

	private static Set<String> leArquivoCNPJs(Optional<Path> arqCNPJ) throws IOException {
		if (arqCNPJ.isPresent()) {
			List<String> listaCNPJs = Files.readAllLines(arqCNPJ.get());
			listaCNPJs.replaceAll(JuntaArquivosPGFN::cnpjParaNumero);

			//Usando Set fica mais rápido do que usando List
			return new HashSet<>(listaCNPJs);
		} else {
			return Collections.emptySet();
		}
	}

	private static void criaBaseIndividual(Path arqSaida, Path dirEntrada, String arquivoOrigem, Collection<String> cnpjs) throws IOException {
		System.out.println("Criando saída individual " + arqSaida);

		try (BufferedWriter saida = Files.newBufferedWriter(arqSaida, CHARSET);) {
			//Primeiro escreve o cabeçalho, copiando de algum arquivo qualquer.
			//Adiciona coluna indicando o arquivo de origem
			escreveCabecalhoBaseIndividual(saida, dirEntrada);

			//Depois escreve os arquivos, pulando a primeira linha, que tem o cabeçalho
			Files.list(dirEntrada).filter(JuntaArquivosPGFN::isCSV).forEach((arqEntrada) -> {
				System.out.println("Processando arquivo " + arqEntrada);

				try (BufferedReader entrada = Files.newBufferedReader(arqEntrada, CHARSET);) {
					entrada.lines().skip(1).forEach((line) -> {
						try {
							String cnpj = cnpjParaNumero(line.substring(0, line.indexOf(';')));

							//Escreve apenas as linhas com CNPJ na lista, incluindo a coluna com o arquivo de origem
							//Se não houver lista de CNPJs, não faz filtragem alguma (escreve todas as linhas)
							if (cnpjs.isEmpty() || cnpjs.contains(cnpj)) {
								saida.write(line);
								saida.write(';');
								saida.write(arquivoOrigem);
								saida.newLine();
							}
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private static void escreveCabecalhoBaseIndividual(BufferedWriter saida, Path dirEntrada) throws IOException {
		Path arquivoQualquer = Files.list(dirEntrada).filter(JuntaArquivosPGFN::isCSV).findAny().get();
		try (BufferedReader entrada = Files.newBufferedReader(arquivoQualquer, CHARSET);) {
			saida.write(entrada.readLine());
			saida.write(";ARQUIVO_ORIGEM");
			saida.newLine();
		}
	}

	private static void criaBaseConsolidada(Path arqFGTS, Path arqPrevidenciario, Path arqNaoPrevidenciario, Path arqSaida) throws IOException {
		try (BufferedWriter saida = Files.newBufferedWriter(arqSaida, CHARSET);) {
			System.out.println("Criando saída consolidada " + arqSaida);

			//Cabeçalho contendo todas as colunas dos três arquivos
			saida.write("CPF_CNPJ;TIPO_PESSOA;TIPO_DEVEDOR;NOME_DEVEDOR;UF_UNIDADE_RESPONSAVEL;UNIDADE_RESPONSAVEL;ENTIDADE_RESPONSAVEL;UNIDADE_INSCRICAO;NUMERO_INSCRICAO;TIPO_SITUACAO_INSCRICAO;SITUACAO_INSCRICAO;RECEITA_PRINCIPAL;TIPO_CREDITO;DATA_INSCRICAO;INDICADOR_AJUIZADO;VALOR_CONSOLIDADO;ARQUIVO_ORIGEM");
			saida.newLine();

			escreveBaseConsolidadaFGTS(arqFGTS, saida);
			escreveBaseConsolidadaPrevidenciario(arqPrevidenciario, saida);
			escreveBaseConsolidadaNaoPrevidenciario(arqNaoPrevidenciario, saida);
		}
	}

	private static void escreveBaseConsolidadaFGTS(Path arqEntrada, BufferedWriter saida) throws IOException {
		try (BufferedReader entrada = Files.newBufferedReader(arqEntrada, CHARSET);) {
			System.out.println("Juntando arquivo FGTS " + arqEntrada);

			//Pula a primeira linha, do cabeçalho
			entrada.lines().skip(1).forEach((linha) -> {
				try {
					String[] componentes = linha.split(";");
					if (componentes.length != 16) {
						throw new RuntimeException("Formato do arquivo FGTS diferente do esperado.");
					}

					//FGTS não tem a coluna 'TIPO_CREDITO'
					saida.write(componentes[0]); saida.write(";"); //CPF_CNPJ
					saida.write(componentes[1]); saida.write(";"); //TIPO_PESSOA
					saida.write(componentes[2]); saida.write(";"); //TIPO_DEVEDOR
					saida.write(componentes[3]); saida.write(";"); //NOME_DEVEDOR
					saida.write(componentes[4]); saida.write(";"); //UF_UNIDADE_RESPONSAVEL
					saida.write(componentes[5]); saida.write(";"); //UNIDADE_RESPONSAVEL
					saida.write(componentes[6]); saida.write(";"); //ENTIDADE_RESPONSAVEL
					saida.write(componentes[7]); saida.write(";"); //UNIDADE_INSCRICAO
					saida.write(componentes[8]); saida.write(";"); //NUMERO_INSCRICAO
					saida.write(componentes[9]); saida.write(";"); //TIPO_SITUACAO_INSCRICAO
					saida.write(componentes[10]); saida.write(";"); //SITUACAO_INSCRICAO
					saida.write(componentes[11]); saida.write(";"); //RECEITA_PRINCIPAL
					saida.write(";"); //TIPO_CREDITO
					saida.write(componentes[12]); saida.write(";"); //DATA_INSCRICAO
					saida.write(componentes[13]); saida.write(";"); //INDICADOR_AJUIZADO
					saida.write(componentes[14]); saida.write(";"); //VALOR_CONSOLIDADO
					saida.write(componentes[15]); //ARQUIVO_ORIGEM
					saida.newLine();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private static void escreveBaseConsolidadaPrevidenciario(Path arqEntrada, BufferedWriter saida) throws IOException {
		try (BufferedReader entrada = Files.newBufferedReader(arqEntrada, CHARSET);) {
			System.out.println("Juntando arquivo Previdenciário " + arqEntrada);

			//Pula a primeira linha, do cabeçalho
			entrada.lines().skip(1).forEach((linha) -> {
				try {
					String[] componentes = linha.split(";");
					if (componentes.length != 14) {
						throw new RuntimeException("Formato do arquivo previdenciário diferente do esperado.");
					}

					//Previdenciário não tem as colunas 'ENTIDADE_RESPONSAVEL', 'UNIDADE_INSCRICAO' e 'TIPO_CREDITO'
					saida.write(componentes[0]); saida.write(";"); //CPF_CNPJ
					saida.write(componentes[1]); saida.write(";"); //TIPO_PESSOA
					saida.write(componentes[2]); saida.write(";"); //TIPO_DEVEDOR
					saida.write(componentes[3]); saida.write(";"); //NOME_DEVEDOR
					saida.write(componentes[4]); saida.write(";"); //UF_UNIDADE_RESPONSAVEL
					saida.write(componentes[5]); saida.write(";"); //UNIDADE_RESPONSAVEL
					saida.write(";"); //ENTIDADE_RESPONSAVEL
					saida.write(";"); //UNIDADE_INSCRICAO
					saida.write(componentes[6]); saida.write(";"); //NUMERO_INSCRICAO
					saida.write(componentes[7]); saida.write(";"); //TIPO_SITUACAO_INSCRICAO
					saida.write(componentes[8]); saida.write(";"); //SITUACAO_INSCRICAO
					saida.write(componentes[9]); saida.write(";"); //RECEITA_PRINCIPAL
					saida.write(";"); //TIPO_CREDITO
					saida.write(componentes[10]); saida.write(";"); //DATA_INSCRICAO
					saida.write(componentes[11]); saida.write(";"); //INDICADOR_AJUIZADO
					saida.write(componentes[12]); saida.write(";"); //VALOR_CONSOLIDADO
					saida.write(componentes[13]); //ARQUIVO_ORIGEM
					saida.newLine();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private static void escreveBaseConsolidadaNaoPrevidenciario(Path arqEntrada, BufferedWriter saida) throws IOException {
		try (BufferedReader entrada = Files.newBufferedReader(arqEntrada, CHARSET);) {
			System.out.println("Juntando arquivo Não Previdenciário " + arqEntrada);

			//Pula a primeira linha, do cabeçalho
			entrada.lines().skip(1).forEach((linha) -> {
				try {
					String[] componentes = linha.split(";");
					if (componentes.length != 14) {
						throw new RuntimeException("Formato do arquivo não previdenciário diferente do esperado.");
					}

					//Não previdenciário não tem as colunas 'ENTIDADE_RESPONSAVEL', 'UNIDADE_INSCRICAO' e 'RECEITA_PRINCIPAL'
					saida.write(componentes[0]); saida.write(";"); //CPF_CNPJ
					saida.write(componentes[1]); saida.write(";"); //TIPO_PESSOA
					saida.write(componentes[2]); saida.write(";"); //TIPO_DEVEDOR
					saida.write(componentes[3]); saida.write(";"); //NOME_DEVEDOR
					saida.write(componentes[4]); saida.write(";"); //UF_UNIDADE_RESPONSAVEL
					saida.write(componentes[5]); saida.write(";"); //UNIDADE_RESPONSAVEL
					saida.write(";"); //ENTIDADE_RESPONSAVEL
					saida.write(";"); //UNIDADE_INSCRICAO
					saida.write(componentes[6]); saida.write(";"); //NUMERO_INSCRICAO
					saida.write(componentes[7]); saida.write(";"); //TIPO_SITUACAO_INSCRICAO
					saida.write(componentes[8]); saida.write(";"); //SITUACAO_INSCRICAO
					saida.write(";"); //RECEITA_PRINCIPAL
					saida.write(componentes[9]); saida.write(";"); //TIPO_CREDITO
					saida.write(componentes[10]); saida.write(";"); //DATA_INSCRICAO
					saida.write(componentes[11]); saida.write(";"); //INDICADOR_AJUIZADO
					saida.write(componentes[12]); saida.write(";"); //VALOR_CONSOLIDADO
					saida.write(componentes[13]); //ARQUIVO_ORIGEM
					saida.newLine();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private static void validaParametroArquivo(Path path) {
		if (!Files.exists(path) || Files.isDirectory(path) || !Files.isReadable(path)) {
			System.err.println("Certifique-se de que o arquivo '" + path + "' existe e há acesso de leitura.");
			System.exit(1);
		}
	}

	private static void validaParametroDiretorioLeitura(Path path) {
		if (!Files.exists(path) || !Files.isDirectory(path) || !Files.isReadable(path)) {
			System.err.println("Certifique-se de que o diretório '" + path + "' existe e há acesso de leitura.");
			System.exit(1);
		}
	}

	private static void validaParametroDiretorioLeituraEscrita(Path path) {
		if (!Files.exists(path) || !Files.isDirectory(path) || !Files.isReadable(path) || !Files.isWritable(path)) {
			System.err.println("Certifique-se de que o diretório '" + path + "' existe e há acesso de leitura e escrita.");
			System.exit(1);
		}
	}

	private static boolean isCSV(Path path) {
		return path.getFileName().toString().toLowerCase().endsWith(".csv");
	}

	private static String cnpjParaNumero(String cnpj) {
		return cnpj
				.replaceAll("[./-]", "")
				.strip();
	}
}
