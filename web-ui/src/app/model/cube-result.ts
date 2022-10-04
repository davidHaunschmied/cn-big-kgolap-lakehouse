export class CubeResult {
  cube: string; // TriG
  nrOfContexts: number;
  nrOfTriples: number;

  constructor(cube: string, nrOfContexts: number, nrOfTriples: number) {
    this.cube = cube;
    this.nrOfContexts = nrOfContexts;
    this.nrOfTriples = nrOfTriples;
  }
}
