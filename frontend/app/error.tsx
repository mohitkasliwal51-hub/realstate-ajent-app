'use client';

import React, { useEffect } from 'react';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('Unhandled App Router Error:', error);
  }, [error]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-900 text-white p-6 text-center space-y-4">
      <div className="w-16 h-16 rounded-2xl bg-red-600/20 border border-red-500/30 flex items-center justify-center text-red-400 font-bold text-2xl">
        !
      </div>
      <h1 className="text-2xl font-bold">Something went wrong</h1>
      <p className="text-xs text-slate-400 max-w-sm">
        An unhandled error occurred in the workspace component.
      </p>
      <button
        onClick={() => reset()}
        className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-lg transition-colors"
      >
        Try again
      </button>
    </div>
  );
}
