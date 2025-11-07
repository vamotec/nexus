package app.mosia.nexus.domain.model.simulation

case class SimulationVersion(major: Int, minor: Int) {
  def increment(): SimulationVersion = copy(minor = minor + 1)
  override def toString: String      = s"v$major.$minor"
}
