/**
 * Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0
 */

export class LPosOptions {
    public static RANK_REDIS_API = "RANK";
    public static COUNT_REDIS_API = "COUNT";
    public static MAXLEN_REDIS_API = "MAXLEN";
    private rank?: number;
    private count?: number;
    private maxLength?: number;

    constructor({
        rank,
        count,
        maxLength,
    }: {
        rank?: number;
        count?: number;
        maxLength?: number;
    }) {
        this.rank = rank;
        this.count = count;
        this.maxLength = maxLength;
    }

    public toArgs(): string[] {
        const args: string[] = [];

        if (this.rank !== undefined) {
            args.push(LPosOptions.RANK_REDIS_API);
            args.push(this.rank.toString());
        }

        if (this.count !== undefined) {
            args.push(LPosOptions.COUNT_REDIS_API);
            args.push(this.count.toString());
        }

        if (this.maxLength !== undefined) {
            args.push(LPosOptions.MAXLEN_REDIS_API);
            args.push(this.maxLength.toString());
        }

        return args;
    }
}
