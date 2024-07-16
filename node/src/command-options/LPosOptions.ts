/**
 * Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0
 */

export class LPosOptions implements LPosOptions {
    public static RANK_REDIS_API = "RANK";
    public static COUNT_REDIS_API = "COUNT";
    public static MAXLEN_REDIS_API = "MAXLEN";
    private rank?: number;
    private count?: number;
    private maxLength?: number;

    public setRank(rank: number): void {
        this.rank = rank;
    }

    public setCount(count: number): void {
        this.count = count;
    }

    public setMaxLength(maxLength: number): void {
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

export class LPosOptionsBuilder {
    private lposOptions: LPosOptions;

    constructor() {
        this.lposOptions = new LPosOptions();
    }

    public rank(rank: number): this {
        this.lposOptions.setRank(rank);
        return this;
    }

    public count(count: number): this {
        this.lposOptions.setCount(count);
        return this;
    }

    public maxLength(maxLength: number): this {
        this.lposOptions.setMaxLength(maxLength);
        return this;
    }

    public build(): LPosOptions {
        return this.lposOptions;
    }
}
