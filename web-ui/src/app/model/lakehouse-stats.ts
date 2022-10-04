export class LakehouseStats {
    totalIndices: number;
    totalContexts: number;
    totalFiles: number;
    totalStoredFileSizeMB: number;


    constructor(totalIndices: number, totalContexts: number, totalFiles: number, totalStoredFileSizeBytes: number) {
        this.totalIndices = totalIndices;
        this.totalContexts = totalContexts;
        this.totalFiles = totalFiles;
        this.totalStoredFileSizeMB = totalStoredFileSizeBytes / 1024 / 1024;
    }
}
