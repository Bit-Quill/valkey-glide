"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const utils_1 = require("./utils");
function fill_database(data_size, host, isCluster, tls, port) {
    return __awaiter(this, void 0, void 0, function* () {
        const client = yield (0, utils_1.createRedisClient)(host, isCluster, tls, port);
        const data = (0, utils_1.generate_value)(data_size);
        yield client.connect();
        const CONCURRENT_SETS = 1000;
        const sets = Array.from(Array(CONCURRENT_SETS).keys()).map((index) => __awaiter(this, void 0, void 0, function* () {
            for (let i = 0; i < utils_1.SIZE_SET_KEYSPACE / CONCURRENT_SETS; ++i) {
                const key = (i * CONCURRENT_SETS + index).toString();
                yield client.set(key, data);
            }
        }));
        yield Promise.all(sets);
        yield client.quit();
    });
}
Promise.resolve()
    .then(() => __awaiter(void 0, void 0, void 0, function* () {
    console.log(`Filling ${utils_1.receivedOptions.host} with data size ${utils_1.receivedOptions.dataSize}`);
    yield fill_database(utils_1.receivedOptions.dataSize, utils_1.receivedOptions.host, utils_1.receivedOptions.clusterModeEnabled, utils_1.receivedOptions.tls, utils_1.receivedOptions.port);
}))
    .then(() => {
    process.exit(0);
});
