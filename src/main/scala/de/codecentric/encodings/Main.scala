package de.codecentric
package encodings

import de.codecentric.domain._
import de.codecentric.persistence.{RaceAlg, RegistrationAlg, RunnerAlg}
import de.codecentric.services.RaceRegistrationService
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.{Server, ServerApp}

import scalaz.concurrent.Task
import scalaz.syntax.equal._

object Main extends ServerApp {
  import PureInterpreter.interpreter

  val srv = new RaceRegistrationService

  override def server(args: List[String]): Task[Server] =
    BlazeBuilder.bindHttp(port = 8080, host = "localhost").mountService(srv.service, "/").start
}

object PureInterpreter extends {
  implicit val interpreter: RunnerAlg[Task] with RaceAlg[Task] with RegistrationAlg[Task] =
    new RunnerAlg[Task] with RaceAlg[Task] with RegistrationAlg[Task] {

      var runnerStore: Map[RunnerId, Runner]  = Map()
      var raceStore: Map[RaceId, Race]        = Map()
      var regStore: Map[RaceId, Registration] = Map()

      override def saveRunner(runner: Runner): Task[Unit] = {
        runnerStore = runnerStore.updated(runner.id, runner)
        Task(())
      }

      override def findRunner(id: RunnerId): Task[Option[Runner]] = Task(runnerStore.get(id))

      override def saveRace(race: Race): Task[Unit] = {
        raceStore = raceStore.updated(race.id, race)
        Task(())
      }

      override def findRace(id: RaceId): Task[Option[Race]] = Task(raceStore.get(id))

      override def findReg(id: RaceId): Task[Option[Registration]] =
        Task(regStore.values.find(_.race.id === id))

      override def saveReg(reg: Registration): Task[Unit] = {
        regStore = regStore.updated(reg.race.id, reg)
        Task(())
      }

      override def newReg(raceId: RaceId): Task[Option[Registration]] = {
        Task(raceStore.get(raceId).map { race =>
          val reg = Registration(race, Vector())
          regStore = regStore.updated(reg.race.id, reg)
          reg
        })
      }
    }
}
