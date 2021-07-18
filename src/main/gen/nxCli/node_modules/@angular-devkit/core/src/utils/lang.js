"use strict";
/**
 * @license
 * Copyright Google LLC All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
// Borrowed from @angular/core
Object.defineProperty(exports, "__esModule", { value: true });
exports.isPromise = void 0;
/**
 * Determine if the argument is shaped like a Promise
 */
// tslint:disable-next-line:no-any
function isPromise(obj) {
    // allow any Promise/A+ compliant thenable.
    // It's up to the caller to ensure that obj.then conforms to the spec
    return !!obj && typeof obj.then === 'function';
}
exports.isPromise = isPromise;
