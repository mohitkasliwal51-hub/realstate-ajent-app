import React from 'react';
import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-900 text-white p-6 text-center space-y-4">
      <div className="w-16 h-16 rounded-2xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 font-bold text-2xl">
        404
      </div>
      <h1 className="text-2xl font-bold">Page Not Found</h1>
      <p className="text-xs text-slate-400 max-w-sm">
        The requested page or property URL could not be found.
      </p>
      <Link
        href="/app"
        className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-lg transition-colors"
      >
        Return to Portal
      </Link>
    </div>
  );
}
