# docker-java-example

Prosty przykład użycia biblioteki docker-java do: pull, create container, start, logi, stop i remove.

Wymagania
- Java 11+
- Maven
- Docker Desktop uruchomiony na Windows (daemon)

Szybkie uruchomienie (PowerShell):

```powershell
cd "C:\Users\mateu\Desktop\CILab\docker-java-example"
# zbuduj jar z zależnościami
mvn clean package
# uruchom
java -jar target\docker-java-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Uwagi
- Jeśli Docker Desktop nie używa standardowego hosta, skonfiguruj zmienne środowiskowe DOCKER_HOST/DOCKER_CERT_PATH itp.
- Jeśli pojawią się problemy z połączeniem, uruchom `docker version` i `docker info` by zweryfikować połączenie.

Wymuszanie połączenia TCP na Windows (jeśli używasz Docker Desktop)

1) Włącz expose daemon na TCP w Docker Desktop (localhost:2375)
- Otwórz Docker Desktop → Settings.
- W zależności od wersji znajdź opcję `Expose daemon on tcp://localhost:2375` (Settings → General) i zaznacz ją, kliknij Apply & Restart.
- Jeśli nie ma tej opcji UI, w Settings → Docker Engine możesz dodać do JSON pole `hosts`:

```json
{
	"hosts": ["npipe:////./pipe/docker_engine", "tcp://0.0.0.0:2375"],
	...
}
```

Po restarcie Docker Desktop demon będzie dostępny na tcp://localhost:2375.

2) Uruchom przykład z wymuszonym DOCKER_HOST (PowerShell):

```powershell
cd "C:\Users\mateu\Desktop\CILab\docker-java-example"
#$Env:DOCKER_HOST = 'tcp://localhost:2375'  # niekonieczne jeśli aplikacja narzuca host
mvn clean package
java -jar target\docker-java-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Bezpieczeństwo: expose demona bez TLS na porcie 2375 jest niebezpieczne w sieciach niezaufanych. Do produkcji lepiej skonfigurować TLS lub korzystać z tunelowania.
