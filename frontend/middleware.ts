import { NextRequest, NextResponse } from 'next/server';

const PLATFORM_DOMAINS = [
  'localhost',
  '127.0.0.1',
  'stayfile.in',
  'www.stayfile.in',
  'staging.stayfile.in',
  'dev.stayfile.in',
];

export function middleware(req: NextRequest) {
  const url = req.nextUrl;
  const host = req.headers.get('host') || '';
  const hostname = host.split(':')[0].toLowerCase();

  // Exclude static assets, Next.js internals, and API requests from rewriting
  if (
    url.pathname.startsWith('/_next') ||
    url.pathname.startsWith('/api') ||
    url.pathname.startsWith('/favicon.ico') ||
    url.pathname.includes('.')
  ) {
    return NextResponse.next();
  }

  // Check if host is a platform domain
  const isPlatform = PLATFORM_DOMAINS.some(
    (d) => hostname === d || hostname.endsWith(`.${d}`)
  );

  let tenantSlugOrDomain: string | null = null;

  if (isPlatform) {
    // Check if there is a subdomain on the platform (e.g. "royal-stays.stayfile.in" or "royal-stays.localhost")
    for (const baseDomain of PLATFORM_DOMAINS) {
      if (hostname.endsWith(`.${baseDomain}`)) {
        const subdomain = hostname.replace(`.${baseDomain}`, '');
        if (subdomain && subdomain !== 'www' && subdomain !== 'app' && subdomain !== 'api') {
          tenantSlugOrDomain = subdomain;
          break;
        }
      }
    }
  } else {
    // Independent custom domain (e.g. "www.royalstays.com" or "royalstays.com")
    tenantSlugOrDomain = hostname;
  }

  // If a tenant domain/subdomain was detected and visitor is requesting root path "/",
  // transparently rewrite to the dedicated showcase page "/p/[organizationSlug]"
  if (tenantSlugOrDomain && url.pathname === '/') {
    url.pathname = `/p/${tenantSlugOrDomain}`;
    return NextResponse.rewrite(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
