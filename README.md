# DadosAbertosPGFN
A Procuradoria Geral da Fazenda Nacional (PGFN), seguindo os preceitos da Lei de Acesso à Informação (LAI), disponibiliza em seu sítio eletrônico a [base de dados completa dos devedores inscritos em dívida ativa da união e do FGTS](https://www.pgfn.fazenda.gov.br/acesso-a-informacao/dados-abertos). Esses arquivos, em formato CSV são separados pelo sistema originário (FGTS, Previdenciário e Não Previdenciário) e por Unidade da Federação (UF).

Para facilitar a análise dos dados, este programa junta todos estes arquivos disponibilizados separadamente em apenas uma base de dados CSV. O programa pode também filtrar os dados por CNPJ, para diminuir o tamanho do arquivo final, reduzindo o escopo de análise.

É possível rodar o executável do programa compilado para Windows ou o arquivo .jar utilizando Java.

Executável:
`JuntaArquivosPGFN <diretório FGTS> <diretório previdenciário> <diretório não previdenciário> <diretório saída>`

Java:
`java -jar JuntaArquivosPGFN.jar <diretório FGTS> <diretório previdenciário> <diretório não previdenciário> <diretório saída>`

Em que <diretório FGTS> é o diretório com os arquivos .csv relativos à base "DÍVIDA FGTS", <diretório previdenciário> contém os arquivos da base "DÍVIDA PREVIDENCIÁRIA (SISTEMA DÍVIDA)", <diretório não previdenciário> contém os arquivos da base "DÍVIDA ATIVA GERAL (SISTEMA SIDA)" e diretório saída é onde serão colocadas as saídas do programa.

Para utilizar a filtragem por CNPJ:

`JuntaArquivosPGFN <arquivo CNPJ> <diretório FGTS> <diretório previdenciário> <diretório não previdenciário> <diretório saída>`

Em que <arquivo CNPJ> é um arquivo contendo a lista de CNPJs de interesse, um por linha.
  
## Exemplo de execução

Para exemplificar a execução do programa, no diretório `exemplos` encontra-se a lista de CNPJs de clubes recreativos do país (clubes de futebol e outros tipos de clube). Em `exemplos/clubes`, os arquivos de saída para este caso, com a base consolidada contendo somente os CNPJs filtrados, de tamanho reduzido e fácil de trabalhar em softwares como LibreOffice e Excel.

## Outros diretórios

No diretório `pgfn` encontra-se o código fonte Java do programa. Em `build` scripts para gerar os arquivos executáveis.
