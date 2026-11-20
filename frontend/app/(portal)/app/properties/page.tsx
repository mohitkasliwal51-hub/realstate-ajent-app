'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Building2, Plus, MapPin, ArrowRight, Layers } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { propertyApi, Property, PropertyType } from '@/lib/api/propertyApi';
import { landlordApi, Landlord } from '@/lib/api/landlordApi';
import { isBrokerMode, isLandlordRequired } from '@/lib/orgMode';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { SearchFilterBar } from '@/components/ui/SearchFilterBar';
import { useToast } from '@/lib/toast/ToastContext';

export default function PropertiesPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const brokerMode = isBrokerMode(user);
  const landlordRequired = isLandlordRequired(user);

  const router = useRouter();
  const [properties, setProperties] = useState<Property[]>([]);
  const [landlords, setLandlords] = useState<Landlord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isOwnerVerified, setIsOwnerVerified] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('ALL');

  const loadProperties = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      const [propsData, landlordsData] = await Promise.all([
        propertyApi.getProperties(),
        brokerMode ? landlordApi.getLandlords().catch(() => []) : Promise.resolve([]),
      ]);
      setProperties(propsData || []);
      setLandlords(landlordsData || []);
    } catch (err) {
      console.error('Failed to load properties', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadProperties();
  }, [user?.organizationId]);

  const handleAddPropertyClick = () => {
    if (!isOwnerVerified) {
      router.push('/app/verification');
      return;
    }
    router.push('/app/properties/new');
  };

  const getBadgeVariant = (type: PropertyType) => {
    switch (type) {
      case 'PG': return 'purple';
      case 'HOSTEL': return 'info';
      case 'FLAT': return 'success';
      case 'COMMERCIAL': return 'warning';
      default: return 'default';
    }
  };

  const filteredProperties = properties.filter((p) => {
    const matchesSearch =
      searchTerm === '' ||
      p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.city?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.address?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesType = filterType === 'ALL' || p.type === filterType;
    return matchesSearch && matchesType;
  });

  const filterOptions = [
    { id: 'ALL', label: 'All', count: properties.length },
    { id: 'PG', label: 'PG', count: properties.filter((p) => p.type === 'PG').length },
    { id: 'FLAT', label: 'Flat', count: properties.filter((p) => p.type === 'FLAT').length },
    { id: 'HOSTEL', label: 'Hostel', count: properties.filter((p) => p.type === 'HOSTEL').length },
    { id: 'COMMERCIAL', label: 'Commercial', count: properties.filter((p) => p.type === 'COMMERCIAL').length },
  ];

  return (
    <div className="space-y-6">
      {/* Header section */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Properties</h1>
          <p className="text-xs text-slate-500 mt-1">
            Manage your real estate portfolio, PG accommodations, commercial spaces, and hostel buildings.
          </p>
        </div>
        <Button onClick={handleAddPropertyClick}>
          <Plus className="w-4 h-4 mr-2" /> Add Property
        </Button>
      </div>

      {/* Search and Filters */}
      <SearchFilterBar
        searchTerm={searchTerm}
        onSearchChange={setSearchTerm}
        placeholder="Search properties by name, city or location..."
        filterOptions={filterOptions}
        activeFilter={filterType}
        onFilterChange={setFilterType}
      />

      {/* Property Cards Grid */}
      {isLoading ? (
        <div className="text-center py-12 text-slate-400 text-sm">Loading properties...</div>
      ) : filteredProperties.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-4">
          <div className="w-12 h-12 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center mx-auto">
            <Building2 className="w-6 h-6" />
          </div>
          <div>
            <h3 className="font-bold text-slate-900">No properties match your filter</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              {properties.length === 0
                ? 'Get started by creating your first property (PG, Hostel, Flat, or Commercial building).'
                : 'Try adjusting your search query or property type filter.'}
            </p>
          </div>
          {properties.length === 0 && (
            <Button onClick={handleAddPropertyClick}>
              <Plus className="w-4 h-4 mr-2" /> Add Property
            </Button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredProperties.map((property) => (
            <Link
              key={property.id}
              href={`/app/properties/${property.id}`}
              className="group bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-lg hover:border-blue-500 transition-all overflow-hidden flex flex-col justify-between"
            >
              <div className="p-6 space-y-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="font-bold text-slate-900 text-lg group-hover:text-blue-600 transition-colors">
                      {property.name}
                    </h3>
                    <p className="text-xs text-slate-500 flex items-center gap-1 mt-1">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      {property.address}, {property.city}
                    </p>
                  </div>
                  <Badge variant={getBadgeVariant(property.type)}>{property.type}</Badge>
                </div>

                {property.description && (
                  <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                    {property.description}
                  </p>
                )}
              </div>

              <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-xs font-semibold text-blue-600 group-hover:text-blue-700">
                <span className="flex items-center gap-1 text-slate-600">
                  <Layers className="w-4 h-4 text-slate-400" /> Manage Units & Inventory
                </span>
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
